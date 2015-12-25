# CheaPhone-Server
Server for the CheaPhone android application

Cheaphone is also called BestOffer [original name] especially in the comments in code.


Designed to work with GNU/Linux but I think could be easily adapted to Windows

Copyright 2014 Bortoli Tomas

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

The server of cheaphone must provide 2 basics services:

-Update market offers to clients that have old offers

-Translate mobile numbers into operators (exploiting TIM sms service at 456 number) and using an internal cache

This project make use of gammu [in form of CLI tool] to send sms through internet keys aka GSM/UMTS modems 

The server implement cryptography and digital signature in network communications
Any message from the server is signed and verified on the client side
Any encrypted message from any source came with some random padding to enforce encryption
A mechanism of symmetric key caching is implemented into the system, to improve performances

More details in the code

*Important* to run the server:

-You need to install a certificate file with name "CA_key.pkcs8.pem" in the cwd of the server

-The file of offers is needed with name "fileOfOffers" in server's cwd


Server software developed by Bortoli Tomas in 2014
