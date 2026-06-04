import type { ComponentProps, FC } from 'react';
import * as DropdownMenuPrimitive from '@radix-ui/react-dropdown-menu';
import { cn } from '@/lib/utils';

const DropdownMenu = DropdownMenuPrimitive.Root;
const DropdownMenuTrigger = DropdownMenuPrimitive.Trigger;
const DropdownMenuGroup = DropdownMenuPrimitive.Group;

const DropdownMenuContent: FC<ComponentProps<typeof DropdownMenuPrimitive.Content>> = ({
  className,
  sideOffset = 6,
  ...props
}) => (
  <DropdownMenuPrimitive.Portal>
    <DropdownMenuPrimitive.Content
      data-slot="dropdown-menu-content"
      sideOffset={sideOffset}
      className={cn(
        'bg-popover text-popover-foreground data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 z-50 min-w-[10rem] origin-(--radix-dropdown-menu-content-transform-origin) overflow-hidden rounded-sm border border-foreground/20 p-1 shadow-md',
        className,
      )}
      {...props}
    />
  </DropdownMenuPrimitive.Portal>
);

const DropdownMenuItem: FC<
  ComponentProps<typeof DropdownMenuPrimitive.Item> & {
    inset?: boolean;
    variant?: 'default' | 'destructive';
  }
> = ({ className, inset, variant = 'default', ...props }) => (
  <DropdownMenuPrimitive.Item
    data-slot="dropdown-menu-item"
    data-inset={inset}
    data-variant={variant}
    className={cn(
      "focus:bg-accent focus:text-accent-foreground data-[variant=destructive]:text-destructive data-[variant=destructive]:focus:bg-destructive/10 [&_svg:not([class*='text-'])]:text-muted-foreground relative flex cursor-default items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-none select-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50 data-[inset]:pl-8 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
      className,
    )}
    {...props}
  />
);

const DropdownMenuLabel: FC<
  ComponentProps<typeof DropdownMenuPrimitive.Label> & { inset?: boolean }
> = ({ className, inset, ...props }) => (
  <DropdownMenuPrimitive.Label
    data-slot="dropdown-menu-label"
    data-inset={inset}
    className={cn('px-2 py-1.5 text-sm font-medium data-[inset]:pl-8', className)}
    {...props}
  />
);

const DropdownMenuSeparator: FC<
  ComponentProps<typeof DropdownMenuPrimitive.Separator>
> = ({ className, ...props }) => (
  <DropdownMenuPrimitive.Separator
    data-slot="dropdown-menu-separator"
    className={cn('bg-foreground/15 -mx-1 my-1 h-px', className)}
    {...props}
  />
);

export {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
};
