import type { FC } from 'react';
import { Moon, Sun } from 'lucide-react';
import { useThemeStore } from '@/theme/store';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface ThemeToggleProps {
  className?: string;
}

/** Sun/Moon switch between light and dark; persisted via the theme store. */
const ThemeToggle: FC<ThemeToggleProps> = ({ className }) => {
  const theme = useThemeStore((s) => s.theme);
  const toggleTheme = useThemeStore((s) => s.toggleTheme);
  const isDark = theme === 'dark';

  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={toggleTheme}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      className={cn('text-muted-foreground hover:text-foreground', className)}
    >
      <span className="relative size-4">
        <Sun
          className={cn(
            'absolute inset-0 size-4 transition-all duration-300',
            isDark ? 'scale-0 -rotate-90 opacity-0' : 'scale-100 rotate-0 opacity-100',
          )}
        />
        <Moon
          className={cn(
            'absolute inset-0 size-4 transition-all duration-300',
            isDark ? 'scale-100 rotate-0 opacity-100' : 'scale-0 rotate-90 opacity-0',
          )}
        />
      </span>
    </Button>
  );
};

export default ThemeToggle;
