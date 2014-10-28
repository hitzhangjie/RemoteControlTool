RemoteControlTool
=================

This repository aims  to provide a more convenient way to control your remote
hosts based on the mature rdp protocol, which has been supported by
Windows\Linux and other operating system.

Recently, more and more people has more than one pcs or notebooks. In some
case, people need to control other pcs or notebooks by the one on hand. We can
realize this by remote desktop, or ssh\telnet and so on.
But sometimes, we use dhcp to offer ip for the pcs or notebooks, and after we
shutdown/start the pcs or notebooks, the ip offered may be changed (even
though we can use MAC for static ip, but obviously, it is not flexible).

And, yesterday, a friend of mine issues this question to me. and i know i can
help him by some technique tricks. I will code a c/s programme, in which
client will send some heartbeat to the server to tell the server it is online.
When the server receives the heartbeat packet, it will record the client
<ip,port> into a special data structure, and then it will broadcast this
<ip,port> msg to the other clients. After that, the other clients can get each
other's <ip>, so they can start the rdp request.

Actually, each of these hosts can both work as a rdp client or rdp server. For
a LINUX host, it will run 'xrdp' as the rdp x11 server. While, on a WINDOWS
host, rdp service will be turned on. With respect to the rdp client, 'remmina'
will be used on LINUX host, while on WINDOWS, the default 'mstsc' will be used.

Ah, i found that, if we use 'krdc' instead of 'remmina', then we can
conveniently start a rdp request through cli. And, That' we want.

I have tested the needed software tools on Ubuntu/Windows platform. The
following work, i will program to realize the whole system.

The architecture of this system is described as following:


          [RDP Server]
            --------                                     --------
                                        <ip-a,port-a>        
    |----->   Host A                             /----->  Host B
    |                                           /
    |       --------                           /         --------
    |           |                             /
    |           |                            /
    |           |                --------   /
   r|           |                          /
   d|           |------------->   Server  
   p|        <ip-a,port-a>     /           \
    |                         /  --------   \
    |                        /               \
    |                       /                 \
    |       --------       /                   \         --------
    |                     /                     \
    |-----   Host C  <---/                       \----->  Host D
                     <ip-a,port-a>        <ip-a,port-a>
            --------                                     --------
          [RDP Client]

    
Take the compatibility into consideration, i will code in JAVA.

Thanks for any suggestion, if you have any suggestion, please email to
hit.zhangjie@gmail.com.
