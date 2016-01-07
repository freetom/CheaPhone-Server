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



The server implement cryptography and digital signature in network communications<br/>
RSA 1024-bit for key exchange, AES 128-bit for symmetric encryption and SHA1+RSA for digital signature.<br/>
Any message from the server is signed and the client verifies it upon arrival; if the message sign isn't valid the connection is closed. Public key of the server is hardcoded in the client.<br/>
Any encrypted message from any [of the two] source come with some random padding to enforce encryption<br/>
A mechanism of symmetric key caching is implemented into the system; to improve performances. It impplies that the same key is used for a series of n consecutive days. Now it's only 4.


More details in the code

*Important* to run the server:<br/>
-You need to install a certificate file with name "CA_key.pkcs8.pem" in the cwd of the server<br/>
-The file of offers is needed with name "fileOfOffers" in server's cwd


Server software developed by Bortoli Tomas in 2014
