import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export type { WithElementRef, WithoutChild, WithoutChildrenOrChild } from 'bits-ui';

export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
