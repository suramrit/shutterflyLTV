Shutterfly Customer LifeTime Value - a Redis Based Approach 

The following is a description for an acquisition and analysis strategy for estimating marketing cost of a valuable shutterfly customer. 

The main focus of this implementation is the use of the Redis <Key,Value> Data Store through the use of Jedis, a Redis Java Client for storing the data to a backend redis server with Jedis Pool which allows a thread safe environment for accessing the data on the redis instance.

Contents:
-Redis!? and other design considerations
-90 Degrees: Techniques for handling corner cases
-*The Thin Ice: Important underlying assumptions
-Modularity,Scalability and future development

Why Redis? 

Although the implentation asked for an in memory store. Redis was a prominent choice because of a)support of Atomic Operations b) Support for DS like strings, hashes, lists, sets, sorted sets c)on-disk persistence, high availability and automatic partitioning. 

The Key Value store also allowed storage of highly nested and related entities in data. So our Customers could be stored as a simple <key,value> of ID and JSON strings (which can be easily parsed at a later stage)while keeping relationship information between users and other events. 

This was achieved by simple using a 'EventType' [Eventkey..] mapping and a 'EventType':<UserKey> [event list.. ] mapping for all the events in which the innocent customer was the foreign key. 

This facilitated mapping the relationships of event viz-a-viz the customer.  

Handling the tricky corner cases!

-Since the ordering of events is not guaranteed, the mapping for an event in redis only keeps the event with the latest timestamp. This makes sure that even if the events arrive out of order, the system represents a model that is up-to-date with the data available till now. 

Event arriving late are not discarded, however they are stored with their unique keys in a separate mapping which can be used in a lter time. 

Important Assumptions:
-When talking of feeding the data structure, the programmer will know the specific data structures are passed and how each event is stored within them. Multitude of data structures means that each such scenario will require an overriden ingest() and TopXSimpleLTVCustomers() methods. 
	-A workaround could be to force consistency through an interface. However since no DS were speficied, this is being documented as somethign that can be in the future. 

-A 'NEW' event with a unique only appears once, it may however appear in any order

-An 'UPDATE' has the same key as a previous 'NEW' event. 

-The Ingest() method is fed a single event at a time. 

-All Events belong to the same time zone. Violation of this assumption means that our LTV calculations are incorrect. 

-In the production implementation the input and output path can be either prescient or given. Currently they are hardcoded in the implementation. 

Modularity:

Ingestion and LTV Calculation were decoupled into seperate packages with each handling the mutually exclusive logic. This also alllows for the scope of further development when new DataStructures need to be included. 

This can be easily done by overriding the alreading existing methods in each of the packages. 


Code Requirements: 
In addition to the jars already included. The code requires a Redis-Server process running for data storage

Conclusion:
Redis posits itself as a viable platform for this problem with its fast and persistence data storage.

However I do concede that in the purview of the coding challenge, the advantages in using Redis might be minimal. 

I would really appreciate any comments/feedback on the design and implementation and it would go a long way in helping me determine any gaps in my knwoledge. 

Thank you for your time!

-Suramrit Singh   