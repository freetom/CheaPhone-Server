# CheaPhone-Server
Server for the CheaPhone android application

Cheaphone is also called BestOffer [original name] especially in the code.


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

The server of cheaphone must provide 2 basics services:<br/>
-Update market offers to clients that have old offers<br/>
-Translate mobile numbers into operators (exploiting TIM sms service at 456 number) and using an internal cache


This project make use of gammu [in form of CLI tool] to send sms through internet keys aka GSM/UMTS modems 



The server implement cryptography and digital signature in network communications. <br/>
Property provided to the communication are: Secrecy, Authentication(of server) and Forward-Secrecy.
RSA 2048-bit for key exchange, AES 256-bit for symmetric encryption and SHA1+RSA(2048-bit) for digital signature.<br/>
Any message from the server is signed and the client verifies it upon arrival; if the message sign isn't valid the connection is closed. Public key used to verify the digital signature made by the server is hardcoded in the client.<br/>
Any encrypted message from any [of the two] sources come with some random padding to enforce encryption<br/>
A mechanism of symmetric key caching is implemented into the system; to improve performances. It impplies that the same symmetric key is used for n consecutive days; without doing the key-exchange again. At the moment the limit is 4 days.
Random padding of random length is added around each encrypted message; to limit KnownPlaintext attacks.


More details in the code

*Important* to run the server:<br/>
-You need to install the private key file with name "private.key.pkcs8" in the cwd of the server [pkcs8 encoding]<br/>
-The file of offers is needed with name "fileOfOffers" in server's cwd

openssl used to generate and encode RSA crypto keys


Server software developed by Bortoli Tomas in 2014
