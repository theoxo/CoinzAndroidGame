# Coinz - the social Android game!

Coinz is a social, location-based game for Android OS
in which you walk around the University of Edinburgh
campus collecting coins. The goal of the game is to
maximize how much gold you have in your bank, which you
get in exchange for trading in these collected coins.
However, you can only trade in so many yourself; in order
to maximize your score, you'll have to exchange coins
with other users, too!
 
## Usage

As this software is intended to be free (and not only in a monetary sense),
you will have to provide the necessary API keys and build the software
yourself.

The easiest way of doing this is to open the source code in Android Studio
on your machine and linking it to your Google Firebase account, as Cloud
Firestore is the database assumed by the source code. You must then also define
a constant MAPBOX_KEY containing a
[Mapbox API key / access token](https://www.mapbox.com/help/how-access-tokens-work/)
.

## License

This software is provided under the GNU GPL license; please see
the LICENSE file.

## Coursework notice

This game was developed for the University of Edinburgh course
Informatics Large Practical 2018/2019. As a result, you may find
other pieces of software floating around the web which were
developed to meet the same requirements (including also being dubbed *Coinz*).
However, please note that I was the sole developer of the software provided here,
other than as is indicated in the ACKNOWLEDGEMENTS file.
