# Cashu
The Cashu protocol defines 3 types of interactions that can happen between a client and a mint, where the client can exchange:

1. bitcoin for ecash tokens (**mint**)
2. ecash tokens for bitcoin (**melt**)
3. ecash tokens for ecash tokens (**split**)

The core goal of the `cashuclient` library is to handle all 3 of those interactions as well as the creation, serialization, and storage of the ecash tokens.
