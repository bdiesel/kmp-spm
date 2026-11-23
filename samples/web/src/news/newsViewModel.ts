import { NewsArticle, NewsResult } from "@bdiesel/space-sdk";
import { errorMessage, sdk, Store } from "../sdk";

/** What the News screen shows. */
export interface NewsUiState {
  articles: NewsArticle[];
  isLoading: boolean;
  error: string | null;
}

/** Thin view model: calls SpaceSdk's use cases and exposes UI state. */
export class NewsViewModel {
  readonly state = new Store<NewsUiState>({ articles: [], isLoading: true, error: null });

  constructor() {
    this.show(sdk.news.latest(20));
  }

  /** Refresh / retry. Keeps the current list visible while loading. */
  refresh(): void {
    this.state.set({ ...this.state.state, isLoading: true, error: null });
    this.show(sdk.news.refresh(20));
  }

  private async show(request: Promise<NewsResult>): Promise<void> {
    const result = await request;
    const current = this.state.state;
    this.state.set(
      result instanceof NewsResult.Success
        ? { articles: result.articles, isLoading: false, error: null }
        : { ...current, isLoading: false, error: errorMessage((result as NewsResult.Failure).error) },
    );
  }
}
