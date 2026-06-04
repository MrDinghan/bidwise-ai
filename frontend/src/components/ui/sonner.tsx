import type { CSSProperties, FC } from 'react';
import { Toaster as Sonner, type ToasterProps } from 'sonner';
import { useThemeStore } from '@/theme/store';

// Follow the app's theme store so toasts match light/dark.
const Toaster: FC<ToasterProps> = ({ ...props }) => {
  const theme = useThemeStore((s) => s.theme);
  return (
    <Sonner
      theme={theme}
      className="toaster group"
      style={
        {
          '--normal-bg': 'var(--popover)',
          '--normal-text': 'var(--popover-foreground)',
          '--normal-border': 'var(--border)',
        } as CSSProperties
      }
      {...props}
    />
  );
};

export { Toaster };
