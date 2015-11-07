# Smart-grid
A JADE multi-agent simulation program of an electricity transmission network, to observe and register the real-time status of the each supplier and simulate the evolution of the market over time according to the defined behaviour of each client.

## Description of the system

There are 3 kinds of agent in the simulation system:
- Fournisseur agent
- Consommateur agent
- Observateur agent
The Observateur agent is just to provide an interface to observe the real-time status of each supplier.

In the electricity transmission grid, there are several suppliers and several consumers, every time when a new consumer entered in the system, he will search for a supplier which providing the lowest price, and then subscribe to it and send the electricity consumed periodically(everyday), when a supplier receives consumed electricity sent by clients, it will response to each of them a calculated bill. After each month a client will change his supplier and try to find another one with the lowest price.

**To evolve...**
The supplier can be smarter and it can change its price intelligently according to the changing of subscribed clients.

## Consommateur agent
Every time a consommateur is generated, it searches the lowest price provided by suppliers in the market, then subscribe to it. The client repeat this process monthly to find the cheapest supplier.
The client sends the consumed electricity to his supplier every day.

## Fournisseur agent
Every time when there is a client asking for the price, he will response to it and if the client wants to subscribe to him, he will then decide whether to use the public transport or to construct a private one.
The supplier sends a bill to each of his clients every day.

## Observateur agent
The Observateur agent is just to show the real-time information for the suppliers, including the number of clients, the amout sold, the turnover, the profit etc.
