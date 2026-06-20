# C4 Model

The [C4 model](https://c4model.com/) describes software architecture at four
levels of zoom. Each level is a map at a different scale: you step *into* one box
from the level above to find the next diagram. Read them top-down.

| Level | Document | Zoom | Audience |
|---|---|---|---|
| 1 | [System Context](L1-system-context.md) | The whole platform as a single box, with its users and the outside systems it talks to. | Anyone — non-technical included. |
| 2 | [Containers](L2-containers.md) | The deployable/runnable units inside the platform: six services, PostgreSQL, Redis, and the protocols between them. | Engineers, ops. |
| 3 | [Components](L3-components.md) | The major building blocks inside each service. | Engineers working in a service. |
| 4 | [Code](L4-code.md) | The hexagonal layering shared by every service, and where the patterns live in the package tree. | Engineers writing code. |

**A note on stopping points.** C4 deliberately does not require all four levels for
every part of a system. Level 4 (code) is documented once, as a *shared pattern*,
because every service follows the identical hexagonal layout — repeating it per
service would add pages without adding information. Where a service deviates, its
L3 entry says so.

Diagrams are Mermaid and render on GitHub. The element styling is consistent
across levels: external actors and systems are distinguished from
platform-internal containers, and the synchronous (REST/gRPC) versus asynchronous
(Redis Streams) edges are labelled on every diagram so the communication style is
never ambiguous.
