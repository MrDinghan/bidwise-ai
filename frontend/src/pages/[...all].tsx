import type { FC } from 'react';
import { Navigate } from 'react-router-dom';

/** Catch-all: send unknown paths home. */
const NotFound: FC = () => <Navigate to="/" replace />;

export default NotFound;
