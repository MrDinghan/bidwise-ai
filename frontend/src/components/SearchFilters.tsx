import type { FC } from 'react';
import {
  CreateListingRequestCategory,
  CreateListingRequestCondition,
  SearchListingsSort,
} from '@/api/generated/model';

interface SearchFiltersProps {
  q: string;
  category: string;
  condition: string;
  minPrice: string;
  maxPrice: string;
  sort: string;
  onChange: (key: string, value: string) => void;
}

const SearchFilters: FC<SearchFiltersProps> = ({
  q,
  category,
  condition,
  minPrice,
  maxPrice,
  sort,
  onChange,
}) => (
  <div className="search-filters">
    <input
      aria-label="Search"
      placeholder="Search listings…"
      value={q}
      onChange={(e) => onChange('q', e.target.value)}
    />
    <select
      aria-label="Category"
      value={category}
      onChange={(e) => onChange('category', e.target.value)}
    >
      <option value="">All categories</option>
      {Object.values(CreateListingRequestCategory).map((value) => (
        <option key={value} value={value}>
          {value}
        </option>
      ))}
    </select>
    <select
      aria-label="Condition"
      value={condition}
      onChange={(e) => onChange('condition', e.target.value)}
    >
      <option value="">Any condition</option>
      {Object.values(CreateListingRequestCondition).map((value) => (
        <option key={value} value={value}>
          {value}
        </option>
      ))}
    </select>
    <input
      aria-label="Minimum price"
      type="number"
      placeholder="Min price"
      value={minPrice}
      onChange={(e) => onChange('minPrice', e.target.value)}
    />
    <input
      aria-label="Maximum price"
      type="number"
      placeholder="Max price"
      value={maxPrice}
      onChange={(e) => onChange('maxPrice', e.target.value)}
    />
    <select aria-label="Sort" value={sort} onChange={(e) => onChange('sort', e.target.value)}>
      {Object.values(SearchListingsSort).map((value) => (
        <option key={value} value={value}>
          {value}
        </option>
      ))}
    </select>
  </div>
);

export default SearchFilters;
