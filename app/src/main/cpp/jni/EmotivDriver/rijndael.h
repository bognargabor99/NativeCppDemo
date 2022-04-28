#ifndef RIJANDAEL_H_
#define RIJANDAEL_H_


typedef unsigned char byte;
typedef unsigned int word32;

typedef struct rijndael_instance {
	int Nk,Nb,Nr;
	byte fi[24],ri[24];
	word32 fkey[120];
	word32 rkey[120];
} RI;

#endif /* RIJANDAEL_H_ */
