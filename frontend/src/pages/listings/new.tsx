import type { FC } from 'react';
import ListingForm from '@/components/ListingForm';
import RequireAuth from '@/auth/RequireAuth';

const NewListingPage: FC = () => (
  <RequireAuth>
    <ListingForm mode="create" />
  </RequireAuth>
);

export default NewListingPage;
