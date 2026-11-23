import "./style.css";
import { newsView } from "./news/newsView";
import { NewsViewModel } from "./news/newsViewModel";
import { pictureOfTheDayView } from "./picture/pictureOfTheDayView";
import { PictureOfTheDayViewModel } from "./picture/pictureOfTheDayViewModel";

// App shell: two tabs, each a view backed by its own view model.
// The news view model is created on first visit, like a lazily shown screen.
const title = document.querySelector<HTMLHeadingElement>("#title")!;
const sections = {
  picture: document.querySelector<HTMLElement>("#picture")!,
  news: document.querySelector<HTMLElement>("#news")!,
};

pictureOfTheDayView(sections.picture, new PictureOfTheDayViewModel());
let newsShown = false;

type Tab = keyof typeof sections;

function show(name: Tab): void {
  document
    .querySelectorAll<HTMLButtonElement>(".tab")
    .forEach((t) => t.setAttribute("aria-selected", String(t.dataset.tab === name)));
  sections.picture.hidden = name !== "picture";
  sections.news.hidden = name !== "news";
  title.textContent = name === "picture" ? "Picture of the Day" : "Space News";
  if (name === "news" && !newsShown) {
    newsShown = true;
    newsView(sections.news, new NewsViewModel());
  }
}

// The URL hash (#picture / #news) selects the tab, so tabs can be linked.
const tabFromHash = (): Tab => (location.hash === "#news" ? "news" : "picture");
document.querySelectorAll<HTMLButtonElement>(".tab").forEach((tab) =>
  tab.addEventListener("click", () => {
    location.hash = tab.dataset.tab!;
  }),
);
window.addEventListener("hashchange", () => show(tabFromHash()));
show(tabFromHash());
