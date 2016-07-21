# Smart-grid
A JADE multi-agent electricity transmission system, to observe the market evolution according to real-time dynamic behaviour of suppliers clients.

## Description of the system

There are 3 kinds of agent in the simulation system:
- Supplier agent
- Consumer agent
- Observer agent
The Observer agent is to provide an interface to observe the real-time status of each supplier.

In the electricity transmission grid, there are several suppliers and several consumers, every time when a new consumer entered in the system, he will search for a supplier which providing the lowest price, and then subscribe to it and send the electricity consumed periodically(everyday), when a supplier receives consumed electricity sent by clients, it will response to each of them a calculated bill. After each month a client will change his supplier and try to find another one with the lowest price.

**To evolve...**
The supplier can be smarter and it can change its price intelligently according to the changing of subscribed clients.

## Consumer agent
Every time a consumer is generated, it searches the lowest price provided by suppliers in the market, then subscribe to it. The client repeat this process monthly to find the cheapest supplier.
The client sends the consumed electricity to his supplier every day.

## Supplier agent
Every time when there is a client asking for the price, he will response to it and if the client wants to subscribe to him, he will then decide whether to use the public transport or to construct a private one.
The supplier sends a bill to each of his clients every day.

## Observer agent
The Observer agent is just to show the real-time information for the suppliers, including the number of clients, the amout sold, the turnover, the profit etc.
