# Split
The split operation consists of sending to the mint a list of valid proofs and asking for signatures on a list of blinded messages, presumably to trade certain denominations for others. For example if you own tokens `[32, 16, 4, 4, 1]` and you want to make a payment for 43 sat, you might request a split on token `[16]` and ask for signatures on new tokens `[8, 4, 2, 2]`, giving you a new token pool of `[32, 8, 4, 4, 4, 2, 2, 1]`. You can then use `[32, 8, 2, 1]` to make your 43 sat payment.  

The split operation is always triggered by a requirement for new denominations, but is unlikely to be called _directly_ by the user. Rather, the split is triggered by the wallet itself when it needs to either build a token or request a [melt](./melt.md) for which it doesn't have the correct denominations. In its simplest form, the wallet would call a split because it would need one specific denomination (say you have [32, 16] and you want to make a 36 sat payment, you just need a split for [4]).  

The workflow would be the following:

1. You need the make a payment for `x` satoshis, and need to know if you have the right denominations to do so. Call `isSplitRequired()`.
2. If you have enough total balance but don't have the correct denominations to compose into your desired total, you need to split.
3. You call split with the new denominations you need, and enough tokens to pay for them.
