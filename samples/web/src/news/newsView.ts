import { NewsArticle } from "@bdiesel/space-sdk";
import { el } from "../dom";
import { NewsUiState, NewsViewModel } from "./newsViewModel";

/** View: renders the view model's state and forwards user actions. */
export function newsView(root: HTMLElement, viewModel: NewsViewModel): void {
  const refresh = el("button", {}, "↻ Refresh");
  const status = el("p", { className: "status" });
  const list = el("ul", { className: "news" });

  refresh.addEventListener("click", () => viewModel.refresh());
  root.replaceChildren(el("div", { className: "toolbar" }, refresh), status, list);

  viewModel.state.subscribe((state: NewsUiState) => {
    refresh.disabled = state.isLoading;
    status.textContent = state.error ?? (state.isLoading && state.articles.length === 0 ? "Loading…" : "");
    status.className = state.error ? "status error" : "status";
    list.replaceChildren(...state.articles.map(articleRow));
  });
}

function articleRow(article: NewsArticle): Node {
  return el(
    "li",
    {},
    el(
      "a",
      { href: article.url, target: "_blank", rel: "noopener" },
      el("span", { className: "headline" }, article.title),
      el("span", { className: "meta" }, `${article.newsSite} · ${article.publishedAt.slice(0, 10)}`),
    ),
  );
}
