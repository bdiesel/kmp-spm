import { SpaceError, SpaceSdk } from "@bdiesel/space-sdk";

/** One SDK instance for the whole app: it owns the network clients and caches. */
export const sdk = new SpaceSdk();

/** The app decides how SDK errors read to its users. */
export function errorMessage(error: SpaceError): string {
  switch (error) {
    case SpaceError.NETWORK:
      return "Couldn't reach the server. Check your connection and try again.";
    case SpaceError.SERVER:
      return "The service had a problem. Try again later.";
    case SpaceError.NOT_FOUND:
      return "There's no picture for that day.";
    case SpaceError.INVALID_DATE:
      return "Pick a date from June 16, 1995 onward.";
    default:
      return "Something went wrong.";
  }
}

/** Minimal observable state, the web equivalent of StateFlow / @Observable. */
export class Store<T> {
  private listeners = new Set<(state: T) => void>();

  constructor(private current: T) {}

  get state(): T {
    return this.current;
  }

  set(next: T): void {
    this.current = next;
    this.listeners.forEach((listener) => listener(next));
  }

  subscribe(listener: (state: T) => void): () => void {
    this.listeners.add(listener);
    listener(this.current);
    return () => this.listeners.delete(listener);
  }
}
