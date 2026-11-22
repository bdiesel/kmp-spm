package com.example.spaceapi

// Real responses recorded on 2026-11-22, trimmed (long HTML fields shortened).

internal const val APOD_LIST_JSON = """
[
 {
  "date": "2026-11-22",
  "post_id": 1514357,
  "title": "Swirls and Colors on Jupiter from Juno",
  "permalink": "https://science.nasa.gov/image-article/apod-2026-november-22-swirls-and-colors-on-jupiter-from-juno/",
  "media_type": "image",
  "explanation": "<strong>Explanation:</strong> What creates the colors in Jupiter's clouds? No one is sure. The thick atmosphere of <a href=\"https://science.nasa.gov/jupiter/\">J...",
  "credit": "<a href=\"https://www.nasa.gov/\">NASA</a>, <a href=\"https://science.nasa.gov/mission/juno/\">Juno</a>, <a href=\"http://www.swri.org/\">SwRI</a>, <a href=\"http://www.msss.com/\">MSSS</a>; <em>Processing &amp; <a href=\"https://creativecommons.org/licenses/by-nc/3.0/\">License</a>:</em> Matt Brealey, Seán Doran",
  "copyright": "<a href=\"https://www.nasa.gov/\">NASA</a>, <a href=\"https://science.nasa.gov/mission/juno/\">Juno</a>",
  "alt": "A busy field of colorful clouds is pictured showing much interesting structure including many cloud swirls.",
  "url": "https://science.nasa.gov/image-article/apod-2026-november-22-swirls-and-colors-on-jupiter-from-juno/",
  "hdurl": "https://assets.science.nasa.gov/dynamicimage/assets/science/cds/apod/apod/2026/november/JupiterSwirls_JunoBrealey_3709.jpg?w=3709&h=2772&fit=clip",
  "basic_html": "<!doctype html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n<meta n...",
  "basic_html_url": "https://science.nasa.gov/wp-json/wp/v2/apod-basic/261122/html"
 },
 {
  "date": "2026-11-21",
  "post_id": 1513943,
  "title": "Voyager's Neptune",
  "permalink": "https://science.nasa.gov/image-article/apod-2026-november-21-voyagers-neptune/",
  "media_type": "image",
  "explanation": "<strong>Explanation:</strong> Cruising through the outer solar system, the Voyager 2 spacecraft made its closest approach...",
  "credit": "Assembly/Processing - Rolf Olsen,<br>Data - Voyager 2",
  "copyright": "Assembly/Processing - Rolf Olsen,<br>Data - Voyager 2",
  "alt": "",
  "url": "https://science.nasa.gov/image-article/apod-2026-november-21-voyagers-neptune/",
  "hdurl": "https://assets.science.nasa.gov/dynamicimage/assets/science/cds/apod/apod/2026/november/Neptune-South-Pole-Voyager-2_2327x1670.jpg?w=2327&h=1670&fit=clip",
  "basic_html": "<!doctype html>...",
  "basic_html_url": "https://science.nasa.gov/wp-json/wp/v2/apod-basic/261121/html",
  "some_future_field": "ignored by the client"
 }
]
"""

internal const val APOD_ERROR_JSON = """
{"code":"rest_invalid_param","message":"Invalid parameter(s): date","data":{"status":400,"params":{"date":"Invalid parameter."},"details":[]}}
"""

internal const val SFN_ARTICLES_JSON = """
{
 "count": 36980,
 "next": "https://api.spaceflightnewsapi.net/v4/articles/?limit=2&offset=2",
 "previous": null,
 "results": [
  {
   "id": 40931,
   "title": "APOD: 2026 November 22 – Swirls and Colors on Jupiter from Juno",
   "authors": [{ "name": "NASA", "socials": null }],
   "url": "https://science.nasa.gov/image-article/apod-2026-november-22-swirls-and-colors-on-jupiter-from-juno/",
   "image_url": "https://assets.science.nasa.gov/dynamicimage/assets/science/cds/apod/apod/2026/november/JupiterSwirls_JunoBrealey_3709.jpg",
   "news_site": "NASA",
   "summary": "APOD Science APOD APOD: 2026 November 22 –…",
   "published_at": "2026-11-22T05:05:00Z",
   "updated_at": "2026-11-22T05:50:09.590997Z",
   "featured": false,
   "launches": [],
   "events": []
  },
  {
   "id": 40930,
   "title": "Live coverage: SpaceX to launch intelligence-gathering satellites for the National Reconnaissance Office",
   "authors": [{ "name": "Will Robinson-Smith", "socials": null }],
   "url": "https://spaceflightnow.com/2026/11/21/live-coverage-spacex-to-launch-intelligence-gathering-satellites-for-the-national-reconnaissance-office-3/",
   "image_url": "https://spaceflightnow.com/wp-content/uploads/2026/04/20260414-Falcon-9-File.jpg",
   "news_site": "Spaceflight Now",
   "summary": "The NROL-136 mission will be the fourth launch of the year...",
   "published_at": "2026-11-21T16:50:51Z",
   "updated_at": "2026-11-21T17:01:12.011033Z",
   "featured": false,
   "launches": [],
   "events": []
  }
 ]
}
"""
