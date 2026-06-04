import type { FC } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Gavel, LogOut, Menu, Package, Plus, User } from 'lucide-react';
import { useAuthStore } from '@/auth/store';
import ThemeToggle from '@/components/ThemeToggle';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

const navBtn = 'font-mono text-xs uppercase tracking-widest';

const AppHeader: FC = () => {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((s) => Boolean(s.token));
  const logout = useAuthStore((s) => s.logout);

  const onLogout = (): void => {
    logout();
    navigate('/');
  };

  return (
    <header className="sticky top-0 z-30 border-b border-foreground/15 bg-background/85 backdrop-blur-md">
      <div className="mx-auto flex h-[4.5rem] max-w-6xl items-center justify-between gap-4 px-4 sm:px-5">
        <Link to="/" className="group flex items-center gap-3">
          <span className="flex size-9 items-center justify-center rounded-sm bg-foreground text-background transition-transform group-hover:-rotate-12">
            <Gavel className="size-4" />
          </span>
          <span className="flex flex-col leading-none">
            <span className="font-display text-xl font-medium tracking-tight">
              BidWise<span className="text-primary">.</span>
            </span>
            <span className="label-mono mt-0.5 hidden text-[0.6rem] tracking-[0.22em] sm:block">
              Student&nbsp;Auction&nbsp;Catalogue
            </span>
          </span>
        </Link>

        {/* Desktop nav */}
        <nav className="hidden items-center gap-1 sm:flex md:gap-2">
          <ThemeToggle />
          <Button asChild variant="ghost" size="sm" className={navBtn}>
            <Link to="/">Browse</Link>
          </Button>
          {isAuthenticated ? (
            <>
              <Button asChild variant="ghost" size="sm" className={navBtn}>
                <Link to="/my-listings">My&nbsp;Lots</Link>
              </Button>
              <Button asChild variant="ghost" size="sm" className={navBtn}>
                <Link to="/profile">Profile</Link>
              </Button>
              <Button asChild size="sm" className={navBtn}>
                <Link to="/listings/new">
                  <Plus className="size-3.5" /> Sell
                </Link>
              </Button>
              <Button variant="ghost" size="icon" onClick={onLogout} aria-label="Log out">
                <LogOut className="size-4" />
              </Button>
            </>
          ) : (
            <>
              <Button asChild variant="ghost" size="sm" className={navBtn}>
                <Link to="/login">Log&nbsp;in</Link>
              </Button>
              <Button asChild size="sm" className={navBtn}>
                <Link to="/register">Join</Link>
              </Button>
            </>
          )}
        </nav>

        {/* Mobile nav: theme toggle + a primary action + a menu with everything else */}
        <div className="flex items-center gap-1.5 sm:hidden">
          <ThemeToggle />
          {isAuthenticated ? (
            <>
              <Button asChild size="sm" className={navBtn}>
                <Link to="/listings/new">
                  <Plus className="size-3.5" /> Sell
                </Link>
              </Button>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" size="icon" className="border-foreground/30" aria-label="Menu">
                    <Menu className="size-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className={navBtn}>
                  <DropdownMenuItem asChild>
                    <Link to="/">Browse</Link>
                  </DropdownMenuItem>
                  <DropdownMenuItem asChild>
                    <Link to="/my-listings">
                      <Package className="size-4" /> My Lots
                    </Link>
                  </DropdownMenuItem>
                  <DropdownMenuItem asChild>
                    <Link to="/profile">
                      <User className="size-4" /> Profile
                    </Link>
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem variant="destructive" onSelect={onLogout}>
                    <LogOut className="size-4" /> Log out
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </>
          ) : (
            <>
              <Button asChild variant="ghost" size="sm" className={navBtn}>
                <Link to="/login">Log&nbsp;in</Link>
              </Button>
              <Button asChild size="sm" className={navBtn}>
                <Link to="/register">Join</Link>
              </Button>
            </>
          )}
        </div>
      </div>
    </header>
  );
};

export default AppHeader;
