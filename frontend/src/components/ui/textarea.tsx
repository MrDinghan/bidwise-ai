import type { ComponentProps, FC } from 'react';
import { cn } from '@/lib/utils';

const Textarea: FC<ComponentProps<'textarea'>> = ({ className, ...props }) => (
  <textarea
    data-slot="textarea"
    className={cn(
      'flex field-sizing-content min-h-16 w-full rounded-md border border-input bg-transparent px-3 py-2 text-base shadow-xs transition-[color,box-shadow] outline-none placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50 md:text-sm',
      'focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]',
      'aria-invalid:ring-destructive/20 aria-invalid:border-destructive',
      className,
    )}
    {...props}
  />
);

export { Textarea };
