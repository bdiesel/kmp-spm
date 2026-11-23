import { PictureOfTheDay, PictureResult } from "@bdiesel/space-sdk";
import { errorMessage, sdk, Store } from "../sdk";

/** What the Picture of the Day screen shows. */
export type PictureUiState =
  | { kind: "loading" }
  | { kind: "shown"; picture: PictureOfTheDay }
  | { kind: "failed"; message: string };

/** Thin view model: calls SpaceSdk's use cases and exposes UI state. */
export class PictureOfTheDayViewModel {
  readonly state = new Store<PictureUiState>({ kind: "loading" });
  private request = 0;

  constructor() {
    this.showLatest();
  }

  showLatest(): void {
    this.load(() => sdk.pictureOfTheDay.today());
  }

  /** [isoDate] is `YYYY-MM-DD`, as produced by `<input type="date">`. */
  showDate(isoDate: string): void {
    const [year, month, day] = isoDate.split("-").map(Number);
    this.load(() => sdk.pictureOfTheDay.forDate(year, month, day));
  }

  showPreviousDay(): void {
    const current = this.state.state;
    if (current.kind !== "shown") return;
    const { year, month, day } = current.picture;
    const previous = new Date(Date.UTC(year, month - 1, day - 1));
    this.load(() =>
      sdk.pictureOfTheDay.forDate(previous.getUTCFullYear(), previous.getUTCMonth() + 1, previous.getUTCDate()),
    );
  }

  /** Only the latest request may update the screen (quick date changes). */
  private async load(request: () => Promise<PictureResult>): Promise<void> {
    const id = ++this.request;
    this.state.set({ kind: "loading" });
    const result = await request();
    if (id !== this.request) return;
    this.state.set(
      result instanceof PictureResult.Success
        ? { kind: "shown", picture: result.picture }
        : { kind: "failed", message: errorMessage((result as PictureResult.Failure).error) },
    );
  }
}
