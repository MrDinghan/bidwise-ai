import type { FC } from 'react';
import { Link } from 'react-router-dom';
import { useGetMyListings } from '@/api/generated/listings/listings';
import RequireAuth from '@/auth/RequireAuth';
import { formatPrice } from '@/lib/format';

const MyListingsContent: FC = () => {
  const { data, isLoading, isError } = useGetMyListings({ size: 50 });

  if (isLoading) {
    return <p>Loading…</p>;
  }
  if (isError || !data) {
    return <p role="alert">Could not load your listings.</p>;
  }

  const listings = data.content ?? [];

  return (
    <main>
      <h1>My listings</h1>
      <p>
        <Link to="/listings/new">+ New listing</Link>
      </p>
      {listings.length === 0 ? (
        <p>You have no listings yet.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Title</th>
              <th>Status</th>
              <th>Price</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {listings.map((listing) => (
              <tr key={listing.id}>
                <td>
                  <Link to={`/listings/${listing.id}`}>{listing.title}</Link>
                </td>
                <td>{listing.status}</td>
                <td>{formatPrice(listing.currentPrice)}</td>
                <td>
                  <Link to={`/listings/${listing.id}/edit`}>Edit</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
};

const MyListingsPage: FC = () => (
  <RequireAuth>
    <MyListingsContent />
  </RequireAuth>
);

export default MyListingsPage;
