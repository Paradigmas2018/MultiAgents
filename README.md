# MultiAgents

System developed based on the multi-agent paradigm. A bus network is developed. 

Buses are agents which communicate people at the next stop that and its passengers that it is arriving, and kicking out passengers who are still in the bus after the route is complete. 

People are also agents, communicating if they are getting into or off the bus, getting kicked off the bus trying to reach a bus stop which is not in the bus line's route or giving up on waiting a bus after waiting for too long

Person is created by: namePerson:agents.PersonAgent(busLineNumber, currentBusStopCode, desiredBusStopCode)
Bus is created by: nameBus:agents.BusAgent(busLineNumber, stopCode1, stopCode2, stopCode3, ...)

namePerson and nameBus are strings. busLineNumber, currentBusStopCode, desiredBusStopCode, stopCode1, stopCode2 and so on are all integers.

Suggested run arguments in eclipse to be able to check all implemented behaviours are: 
```
-gui jade.Boot;UM:agents.PersonAgent(234,2,4);DOIS:agents.PersonAgent(234,1,3);
TRES:agents.PersonAgent(234,3,4);QUATRO:agents.PersonAgent(234,1,7);
CINCO:agents.PersonAgent(255,1,3);ONIBUS:agents.BusAgent(234,1,2,3,4)
```

If you would like to be able to sniff the conversations between agents, you can remove the bus from the arguments to avoid conversations before you set up the sniffer agent. Then, after setting it up, you can create the bus agent via the JADE interface.

ps.: O repositório quase não possui commits, pois como fi-lo sozinho estava fazendo-o localmente e subi-lo para o Github somente para a apresentação e entrega.
