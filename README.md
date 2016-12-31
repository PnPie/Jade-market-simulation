# Smart-Grid
A JADE multi-agent market simulation system to observe the market evolution and how does every agent behave.

## Description of the system

There are 3 kinds of agent in the simulation system:
- Supplier agent
- Consumer agent
- Observer agent

## Consumer agent
Subscribe to a supplier with lowest price other than the current one, pay every month, and change supplier every 3 month.

## Supplier agent
Send bill to its clients, if it get profits for four successive month, increase price by 10%, if not, decrease price by 10%.

## Observer agent
Communicate with all suppliers every month to retrieve their information. (Clients number, profit, balance).
