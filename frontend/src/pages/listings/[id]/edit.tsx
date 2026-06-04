import type { FC } from 'react';
import { useParams } from 'react-router-dom';
import ListingForm from '@/components/ListingForm';
import RequireAuth from '@/auth/RequireAuth';

const EditListingPage: FC = () => {
  const { id } = useParams();

  return (
    <RequireAuth>
      <ListingForm mode="edit" listingId={Number(id)} />
    </RequireAuth>
  );
};

export default EditListingPage;
