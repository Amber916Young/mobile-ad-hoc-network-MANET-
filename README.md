# Bluetooth MANET 
## Using Bluetooth to realise MANET model, and define a communication protocol to realise multi-hop comm.

## Description
The program uses in Android devices, Users can change the cloud URL to access the cloud service.
<img width="1212" alt="image" src="https://user-images.githubusercontent.com/31204783/186411404-0300d90a-1a85-4178-8755-0e29aab7d044.png">
The project name is "Cloud platform for the management of mobile ad-hoc networks", it can be decomposed into two parts, one is about cloud management,
and another one is to realise an ad-hoc network. As we know, 
There are many kinds of research on MANET,and a significant proportion of these have been done on various types of simulators, mainly due to the MANET protocol being very cumbersome and difficult to implement. There are differences between the simulated environment and situation and the actual results, as the implementation of the MANET network in the first step is very necessary. Mobile ad-hoc network, the keyword is mobile, Android platform supports Bluetooth network stack, this function can realise exchange data within Bluetooth devices through a wireless way. Within a radio range, mobile devices can establish channels to transfer any kind of data with each other without infrastructure support.


## Traditional MANET Chanlleges
For traditional MANETs there are many challenges in managing MANETs as the mobility of mobile devices and the scarcity of battery resources lead to dynamic changes in the network structure and if routing tables are not updated immediately then data transmission will be interrupted and data will be lost. Consider a scenario in which some devices in a MANET suddenly shut down due to battery drain and the communication link is broken. Since the local routing table of the devices has not been updated in time, the communication between the devices will result in data loss because the link does not exist (broken link). In the second scenario, when the number of non-directly devices increases, it is slow to determine membership information and update the routing table by transmitting messages between intermediate devices, and it takes a lot of time to update the routing table between members once a device leaves the MANET. In some cases, the interruption of the link as a bridge can be seen as a splitting of the MANET, since the devices are not paired with each other even if they are in the same radio range. In the last scenario, in multi-hop communication, data will likely be discarded during transmission because of the mobility of the devices. Also when the number of hops increases, the message latency is higher, the possibility of losing data is higher and the reliability of the message is lower, which is a chain reaction.
## Scenario
It can be used for communication in network-free environments, such as zoos, amusement parks, and other attractions where the network is crowded, and for group chats with family and friends.

#### Two cloud services
1. Using graph algorithms to form network topology diagrams to update routing tables in real-time for each mobile device in a faster and more accurate way, even if some devices are not connected to the network, they can be updated by connected neighbouring devices, which greatly increases the speed of routing table updates, and also when some devices lose their signal, the cloud can update the network structure in a short time, reducing the probability of routing table errors.
2. Use the cloud database to store transmission data. Although this does not require any logic to be used, it is very effective in multi-hop communication to ensure that messages are ready to be delivered to and returned from the destination.
#### Operations of MANET
1. Split/merge a MANET
#### Operations of devices
1. Join/leave a MANET

### Both operations based on radio distance, the user does not need to do anything, the cloud will automatically recognize that the user is in which MANET

## UI
<img width="1469" alt="image" src="https://user-images.githubusercontent.com/57694784/186414129-65aba94b-dc88-4db8-82ad-5cf9f9925b15.png">
<img width="1602" alt="image" src="https://user-images.githubusercontent.com/57694784/186414051-25d5ba16-b569-45b7-81dd-eb94948f785e.png">

## Communication Object
<img width="652" alt="image" src="https://user-images.githubusercontent.com/57694784/186415137-603a29dd-e23b-4a38-b962-c47c4ef00ebb.png">

### The detail project is uploaded in my personal blog
http://www.youngbird97.top/

