import type { FC } from 'react';
import { Search } from 'lucide-react';
import {
  CreateListingRequestCategory,
  CreateListingRequestCondition,
  SearchListingsSort,
} from '@/api/generated/model';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

interface SearchFiltersProps {
  q: string;
  category: string;
  condition: string;
  minPrice: string;
  maxPrice: string;
  sort: string;
  onChange: (key: string, value: string) => void;
}

// Radix Select forbids empty-string item values, so the "any" option uses a
// sentinel that maps back to '' (the contract the marketplace page expects).
const ANY = '__any__';

const triggerClass = 'w-full rounded-sm border-foreground/20 font-mono text-xs uppercase tracking-wider';

const SearchFilters: FC<SearchFiltersProps> = ({
  q,
  category,
  condition,
  minPrice,
  maxPrice,
  sort,
  onChange,
}) => (
  <div className="border border-foreground/15 bg-card/60 p-3 backdrop-blur-sm">
    <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-6">
      <div className="relative sm:col-span-2">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          aria-label="Search"
          placeholder="Search the catalogue…"
          className="rounded-sm border-foreground/20 pl-9"
          value={q}
          onChange={(e) => onChange('q', e.target.value)}
        />
      </div>

      <Select
        value={category || ANY}
        onValueChange={(value) => onChange('category', value === ANY ? '' : value)}
      >
        <SelectTrigger aria-label="Category" className={triggerClass}>
          <SelectValue placeholder="All categories" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ANY}>All categories</SelectItem>
          {Object.values(CreateListingRequestCategory).map((value) => (
            <SelectItem key={value} value={value}>
              {value}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select
        value={condition || ANY}
        onValueChange={(value) => onChange('condition', value === ANY ? '' : value)}
      >
        <SelectTrigger aria-label="Condition" className={triggerClass}>
          <SelectValue placeholder="Any condition" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ANY}>Any condition</SelectItem>
          {Object.values(CreateListingRequestCondition).map((value) => (
            <SelectItem key={value} value={value}>
              {value}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <div className="grid grid-cols-2 gap-2">
        <Input
          aria-label="Minimum price"
          type="number"
          placeholder="Min $"
          className="rounded-sm border-foreground/20 font-mono"
          value={minPrice}
          onChange={(e) => onChange('minPrice', e.target.value)}
        />
        <Input
          aria-label="Maximum price"
          type="number"
          placeholder="Max $"
          className="rounded-sm border-foreground/20 font-mono"
          value={maxPrice}
          onChange={(e) => onChange('maxPrice', e.target.value)}
        />
      </div>

      <Select value={sort} onValueChange={(value) => onChange('sort', value)}>
        <SelectTrigger aria-label="Sort" className={triggerClass}>
          <SelectValue placeholder="Sort" />
        </SelectTrigger>
        <SelectContent>
          {Object.values(SearchListingsSort).map((value) => (
            <SelectItem key={value} value={value}>
              {value}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  </div>
);

export default SearchFilters;
