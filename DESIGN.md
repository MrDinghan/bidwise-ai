# BidWise AI — Design Document

> One-line positioning: a real-time auction marketplace for the TUS / international-student
> community, using AI to solve the three big pain points — "don't want to write
> descriptions, don't know how to price, afraid of getting ripped off."
> Naming rationale: `AI` is made explicit in the name; `Wise` maps to the core AI
> capability — smart pricing and bidding suggestions.

---

## 1. MVP Scope (nail it down first, avoid spreading too thin)

**In scope:** user registration/login → seller smart listing via photos → item list
search/filter/sort → real-time bidding (anti-snipe) → bid deposit pre-authorization →
automatic settlement on close → email/SMS notifications → AI valuation/bidding assistant
→ AI content moderation → back-office review queue.

**Out of scope for now (defer to v2):** real logistics/shipping (only "pickup point /
in-person handoff" + status transitions), chat/IM, ratings & reviews, multi-language,
mobile app (responsive web first).

> Principle: **get the hardest main line — bidding + payment — to production grade
> first**, then add the AI trio in stages; don't spread everything out at once.

---

## 2. Functional Requirements (including non-CRUD complexity)

### Seller
- List by photo (AI auto-fills title/category/description/suggested start price; seller
  can edit before publishing)
- Set start price, bid increment, auction duration
- View live bids; collect payment after the auction closes

### Buyer
- Browse / **search / filter / sort / advanced search** (keyword, category, price range,
  distance, time remaining, condition)
- **Semantic search (optional):** embeddings for "find things in natural language"
- Bid (must complete deposit pre-authorization first) / set a proxy-bidding cap
- Watchlist, outbid alerts
- AI valuation reference: "is this thing worth the price?"

### System / Back-office
- Anti-snipe automatic time extension
- Automatic settlement on close (charge the winner, release the rest)
- AI content moderation + human review queue, appeals
- Audit logs, operations dashboard (optional)

---

## 3. AI Feature Design (focus area)

> General principle: **all AI output goes through strict JSON + is human-editable
> (human-in-the-loop) + anything price-related always retrieves real historical data
> for the model to reference — never let the model quote a price out of thin air.**
> This is the key differentiator from a "ChatGPT wrapper."

### ① AI Smart Listing (flagship)
- **Input:** 1–N photos + optional seller notes.
- **Processing:** a vision LLM identifies the item → outputs
  `{title, category, condition, description, suggested_start_price, price_range, confidence}`.
- **Prices must not be fabricated:** first retrieve "comparable sold items of the same
  kind" from your own database (RAG), inject those comparable sale prices into the prompt,
  and have the model base its suggestion on real data — annotate in the UI "based on N
  similar sales."
- **Fallback:** the seller can edit all fields before publishing; on low confidence,
  prompt the seller to confirm.

### ② AI Valuation / Bidding Assistant (flagship, echoes "BidWise")
- **Buyer side:** show a "fair-value range" (also based on retrieved comparable sales),
  and judge whether the current bid is "high / reasonable / a good deal."
- **Proxy bidding:** the buyer sets a cap, the system auto-raises up to it; the AI can
  suggest a "rational cap."
- Note: valuation is a suggestion, not a promise — the UI copy must make this clear.

### ③ AI Content Moderation (bonus, leverages your content-governance experience)
- New listing submitted → sent for async review → the LLM classifies text + images
  (prohibited / dangerous goods / fraudulent / duplicate / abusive).
- RAG-retrieve the platform policy clauses so the model **gives a rationale for its
  decision**, not just a label.
- Risk hit → into the human review queue (back-office); pass → auto-published. This
  "AI pre-screen + human backstop + appealable" workflow is an interview highlight.

### Technical implementation notes
- Use the Anthropic API (which you're already learning), tool use / structured output.
- **Cost & stability:** cache recognition results in Redis, rate-limit LLM calls; the
  parsing layer must tolerate occasional non-conforming JSON from the model (validate +
  retry + degrade).
- **Testability:** don't assert the LLM's specific output; test the two layers of
  "prompt assembly" and "output parsing/validation" using mocked model responses.

---

## 4. Architecture & Tech-stack Mapping

| Component | Technology | What it does in this project |
|---|---|---|
| Frontend | React + TypeScript | list/bidding pages, live bidding UI, smart-listing form, back-office review console |
| Backend | Spring Boot (Java) | auction engine, settlement, users, AI orchestration, Stripe integration |
| Cache/Concurrency | Redis | live highest price, atomic bid lock, anti-snipe timing, close-due queue, rate limiting, AI result cache |
| Messaging | Apache Kafka | bid-event fan-out, close→settlement, payment events, submit-for-review / review results |
| Payment | Stripe (test mode) | deposit pre-auth (auth), charge winner on close (capture), release on loss (void) |
| Real-time | WebSocket (STOMP/SockJS, reusing ChatNest experience) | push live highest price, outbid alerts |
| Notifications | Email (e.g. SendGrid) + SMS (e.g. Twilio) | outbid / won / payment / review results |
| AI | Anthropic API + vector retrieval | smart listing, valuation, moderation |
| Containers/Deploy | Docker + AWS (optional) | imaging, cloud deploy |
| CI/CD | Jenkins (local) + GitHub | PR-triggered build, test, image build |

---

## 5. Data Model (main entities)

```
User(id, name, email, passwordHash, role[USER/ADMIN], stripeCustomerId, createdAt)

Listing(id, sellerId→User, title, category, description, condition,
        photos[], startPrice, bidIncrement, currentPrice, currentBidderId,
        status[DRAFT/PENDING_REVIEW/ACTIVE/CLOSED/SOLD/REJECTED],
        startAt, endAt, pickupLocation(geo), createdAt)

Bid(id, listingId→Listing, bidderId→User, amount, isProxy, proxyMax,
    status[ACTIVE/OUTBID/WON/LOST], paymentHoldId, createdAt)

PaymentHold(id, userId, listingId, stripePaymentIntentId, amount,
            status[AUTHORIZED/CAPTURED/VOIDED], idempotencyKey, createdAt)

Settlement(id, listingId, winningBidId, finalAmount,
           status[PENDING/CAPTURED/FAILED], createdAt)

ModerationResult(id, listingId, decision[PASS/FLAG/REJECT],
                 categories[], rationale, reviewedBy, reviewedAt)

WatchList(userId, listingId, createdAt)

SoldComparable  // derived from CLOSED/SOLD Listings, for valuation RAG retrieval
```

---

## 6. The Three Hardest Core Sequences

### (a) Real-time bidding + anti-snipe extension
1. Buyer clicks bid → backend validates: does the user have a valid deposit pre-auth? Is
   the bid > `currentPrice + bidIncrement`? Is the auction still ACTIVE?
2. **Atomic update:** use a Redis Lua script (single-threaded, atomic) on `auction:{id}`
   to compare-and-write the new highest price + new highest bidder. Avoids two concurrent
   bids reading the same stale value.
3. New highest price written successfully → persist the Bid, mark the previous highest
   bidder OUTBID, emit `bid.placed` to Kafka.
4. **Anti-snipe:** if the bid occurs within < 2 minutes of `endAt`, automatically extend
   `endAt` by 2 minutes and update the expiry time in the Redis close-due queue.
5. WebSocket pushes the new highest price to that room.

### (b) Bid deposit pre-authorization (auth hold)
- Before a user participates in an auction for the first time, create a Stripe
  PaymentIntent (`capture_method = manual`) → freeze a fixed deposit (not charged
  immediately).
- Use an **idempotencyKey** to prevent duplicate creation; record it in `PaymentHold`.
- The server validates the amount and **never trusts the price sent by the frontend**.

### (c) Automatic settlement on close
1. A Redis close-due queue (ZSET, score = `endAt`) is polled by a worker via
   `ZRANGEBYSCORE` to pick up due auctions.
2. Emit `auction.closed` to Kafka.
3. Settlement consumer: **capture** the winner (charge the deposit or use a saved payment
   method to charge the final price), **void/cancel** the other bidders to release their
   deposits.
4. Generate a Settlement, emit `payment.events`; the notification service sends
   "won / lost / sold (seller)."

---

## 7. Redis Usage Checklist
- `auction:{id}` (HASH): highBid / highBidder / endAt / version — atomic bid
  compare-and-write (Lua).
- Close-due queue (ZSET): member = auctionId, score = endAt — worker polls to trigger
  close.
- Bid rate limiting: `ratelimit:{userId}` — anti-abuse.
- AI result cache: `ai:listing:{hash}` — repeat recognition of the same image hits cache,
  saving tokens.
- WebSocket presence/room helpers.

---

## 8. Kafka Event Streams

| Topic | Producer | Consumer / Purpose |
|---|---|---|
| `bid.placed` | auction engine | notifications (outbid), read-model updates, audit |
| `auction.closed` | close worker | settlement service (capture/void), notifications |
| `payment.events` | Stripe webhook receiver | settlement persistence, fulfillment status, notifications |
| `listing.submitted` | listing service | AI moderation consumer |
| `moderation.completed` | moderation service | publish/reject, notify seller |

---

## 9. Payment Design (Stripe)
- **Flow:** entry deposit pre-auth (manual capture) → void to release on loss → capture
  on win (or charge the final price with a saved PM, off_session).
- **Idempotency:** create PaymentIntent with an idempotencyKey; **webhook handling must be
  idempotent** — store processed event ids in Redis/DB for dedup, so redelivery doesn't
  double-charge / double-fulfill.
- **Amount validation:** always trust server-side data.
- **Webhook:** receive `payment_intent.*` → convert to `payment.events` into Kafka →
  downstream fulfillment + notifications.
- Use **test mode** throughout; resume phrasing: "implemented a complete
  pre-auth–capture–release flow with idempotent webhook fulfillment" — true and
  sufficient.
- **Bounded auction duration (decided in P3):** a Stripe pre-auth hold expires after
  ~7 days, so an auction must finish before the winner's deposit hold lapses —
  otherwise capture-on-close fails. Therefore the auction window must be bounded, and
  the cap must stay within the hold's validity (target **≤ 5 days**, leaving margin).
  See the listing-duration change below.

---

## 10. Notification Trigger Points
Outbid, auction-ending-soon reminder, won, lost, sold (seller), payment success/failure,
review result (pass/reject). Use templates for email; add SMS for key events (won /
payment).

---

## 11. Real-time Layer
WebSocket (STOMP over SockJS) push: one topic/room per auction, broadcasting the highest
price and time remaining; outbid notifications are pushed directly to the previous highest
bidder. **Directly reuse your ChatNest Socket.io experience** — interview narrative:
"migrating real-time capability from chat to bidding and money-handling scenarios."

---

## 12. Testing Strategy & Coverage (target 90%+)

**Backend unit tests:** JUnit5 + Mockito. Focus on the auction engine (concurrent
bidding, anti-snipe boundaries), settlement logic, valuation service (mock the LLM).
**Backend integration tests:** Testcontainers spins up real Postgres + Redis + Kafka,
running the full "bid → close → settle" chain; stub Stripe with WireMock/stripe-mock, and
**specifically test webhook idempotency** (the same event delivered twice charges only
once).
**Frontend:** Vitest + React Testing Library, mock the API, test the bidding UI and
optimistic updates.
**AI part:** don't assert specific model output; test the two layers of "prompt assembly +
output parsing/validation."
**Coverage:** JaCoCo (backend) + vitest coverage (frontend); CI fails below the threshold.

> Honestly: 90% across all modules is hard. Recommendation: **enforce 90%+ on the core
> domain (bidding/settlement/payment)**, and keep AI and peripheral modules reasonable.

---

## 13. Code Standards & CI/CD

**Standards:** backend Checkstyle + SpotBugs (in the IDE + CI, fail on violation);
frontend ESLint (in CI).

**Jenkins pipeline (local, triggered by GitHub PRs) stages:**
1. Checkout
2. Backend build (Maven/Gradle)
3. Checkstyle + SpotBugs (fail on violation)
4. Unit tests + JaCoCo (fail below threshold)
5. Integration tests (Testcontainers)
6. Frontend `npm ci` → ESLint → vitest + coverage
7. Build Docker images
8. (Optional) push images + deploy to AWS

> Recommendation: **set up CI/CD in stage 1**, not as a last-minute add-on — early
> integration saves rework later.

---

## 14. Development Milestones (depth-first)
- **P0:** skeleton + auth + Docker + Jenkins dry-run working (stand up the engineering
  foundation first)
- **P1:** Listing CRUD + search/filter/sort (non-CRUD logic)
- **P2:** real-time bidding + Redis atomic lock + anti-snipe + WebSocket (the soul of the
  project)
- **P3:** deposit pre-auth + automatic settlement on close + idempotent webhook (the
  payment highlight)
  - **Listing duration model change (do together with P3):** today a listing takes an
    arbitrary future `endAt` (validated only as `@Future`), so a 1-second or 1-year
    auction is accepted — unreasonable for bidders, and incompatible with the ~7-day
    Stripe hold expiry (see §9). Change the seller input from a raw `endAt` timestamp
    to a **duration choice** (e.g. `durationHours`, or a fixed enum of 1/3/5-day
    options) bounded by `@Min/@Max`; the backend computes `endAt = startAt + duration`
    on publish (server-authoritative). Cap the max within the Stripe hold validity
    (≤ 5 days). Contract-first impact: this changes the API, so regenerate the frontend
    client and switch the create/edit form from a date picker to a duration selector.
    Interim mitigation in P2: add a simple max-duration guard on `endAt` to close the
    "1-year auction" hole until the full change lands.
- **P4:** AI smart listing
- **P5:** AI valuation/bidding assistant + AI content moderation + review console
- **P6:** fill in test coverage, notifications, cloud deploy

---

## 15. Resume / Interview Highlights
- Consistency in high-concurrency bidding (Redis Lua atomic compare, anti-snipe)
- Auth-and-capture payments + idempotent webhooks + server-side amount validation (an
  order of magnitude beyond "add a pay button")
- Event-driven architecture (Kafka decouples bidding/settlement/notification/moderation)
- Engineering AI in production: retrieval-augmented pricing (no fabricated quotes), an
  AI-pre-screen + human-backstop moderation workflow
- Real-time capability migration (ChatNest → bidding)
- Complete CI/CD + high coverage + code standards

---

## 16. Risks & Trade-offs
- **Concurrency correctness** is the core difficulty: use atomic operations (Lua/optimistic
  locking) and write concurrency tests.
- **Payment edge cases:** auth expiry, capture failure, duplicate webhooks — all need
  fallbacks.
- **Don't let AI pricing overstep:** always base it on real comparable sales + annotate the
  rationale + keep it human-editable, to avoid misleading users and "hallucinated quotes."
- **Scope control:** advance strictly by milestone; don't touch P4+ before P2/P3 are
  solid.
