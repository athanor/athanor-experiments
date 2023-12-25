C     
C     Last update: 28.04.1997 by Petra Schwerin
C                                Martin-Luther-University Halle-Wittenberg
C
      PROGRAM BPPG
C
      PARAMETER (MAXN=10000,MAXD=10000)
      INTEGER   N,C,Z,NUMBER,DITEM,ITL,ITQ,I,J,DUM
      REAL      V1,V2
      DIMENSION ITL(MAXN),ITQ(MAXN),DUM(MAXN)
C
      READ(5,*,END=888) N,C,V1,V2,NUMBER,Z
  888 CONTINUE
      IF (NUMBER.EQ.0) PAUSE 'Abort: no problems to generate'
      IF (N.GT.MAXN) PAUSE 'Abort : number of items to large'
      OPEN(9,FILE='SDAT')
      DO 999 I=1,NUMBER
         CALL BPPGEN(N,C,V1,V2,Z,DITEM,ITL,ITQ,DUM)
         IF (DITEM.GT.MAXD) PAUSE 'Warning : number of different 
     1                                       items to large'
         WRITE(9,1000) I,DITEM,C
 1000    FORMAT('''','BPP',I6,'''',/,I8,/,I8)
         DO 100 J=1,DITEM
            WRITE(9,2000) ITL(J),ITQ(J)
 2000       FORMAT(I8,2X,I8)
  100    CONTINUE
  999 CONTINUE
      STOP
      END


      SUBROUTINE BPPGEN(N,C,V1,V2,Z,M,WU,Q,DUM)
C
C     Generates an instance of the one-dimensional Bin-Packing 
C     Problem. The format of the resulting problem instance is 
C     identical to that of the generator CUTGEN1 
C     (cf. Gau and Waescher (1995))
C
C     uses ran0, hpsort from Numerical Recipes Software
C
C     In     N     : number of items to be packed, N <= 1000
C            C     : capacity of the bin
C            V1    : lower bound for the relative weight of items
C                    as a fraction of the bin capacity
C            V2    : upper bound for the relative weight of items
C                    as a fraction of the bin capacity
C                    0 < V1 <= V2 <= 1 
C            DUM   : dummy-vector (integer) of size n
C     InOut  Z     : seed for initializing the random number generator
C     Out    M     : number of different item weights
C            WU    : (1..M) item weights  
C            Q     : (1..M) number of items with weight WU(i), i = 1,..,M
C
      INTEGER   Z,C,M,WU,Q,I,II,N,DUM
      REAL      V1,V2,RAN0,ZZZ
      DIMENSION WU(N),Q(N),DUM(N)
C
C     testing if 0 < V1 <= V2 <= 1, otherwise abort
      IF ((V1.LT.0).OR.(V1.GT.V2).OR.(V2.GT.1)) GOTO 999
C     generate N pseudo-random numbers in [V1*C und V2*C]
      DO 100 I=1,N
         ZZZ=RAN0(Z)
         DUM(I)=INT((V1+(V2-V1)*ZZZ)*FLOAT(C)+ZZZ)
  100 CONTINUE
C     sort dummy-vector in non-increasing order 
      CALL HPSORT(N,DUM)
C     sort items in WU, sum up repeated item weights
      II=1
      WU(II)=DUM(N)
      Q(II)=1
      DO 200 I=N-1,1,-1
         IF (DUM(I).EQ.WU(II)) THEN
            Q(II)=Q(II)+1
         ELSE
            II=II+1
            WU(II)=DUM(I)
            Q(II)=1
         ENDIF
  200 CONTINUE
C     register number of different item weights
      M=II
  999 CONTINUE
      RETURN
      END
      
      FUNCTION ran0(z)
C     Copr. 1986-92 Numerical Recipes Software '!5.
C     ******************************************************************
C     ***          Determination of Uniform Random Numbers           ***
C     ******************************************************************
C      
      INTEGER z,IA,IM,IQ,IR
      REAL ran0,AM
      PARAMETER (IA=16807,IM=2147483647,AM=1./IM,IQ=127773,IR=2836)
      INTEGER k
      k=z/IQ
      z=IA*(z-k*IQ)-IR*k
      if (z.LT.0) z=z+IM
      ran0=AM*z
      return
      END

      SUBROUTINE hpsort(n,ra)
C     Copr. 1986-92 Numerical Recipes Software '!5.
C     ******************************************************************
C     ***                Sorting Routine "Heapsort"                  ***
C     ******************************************************************
C
      INTEGER ra(n),rra
      INTEGER i,ir,j,l
      if (n.lt.2) return
      l=n/2+1
      ir=n
10    continue
        if(l.gt.1)then
          l=l-1
          rra=ra(l)
        else
          rra=ra(ir)
          ra(ir)=ra(1)
          ir=ir-1
          if(ir.eq.1)then
            ra(1)=rra
            return
          endif
        endif
        i=l
        j=l+l
20      if(j.le.ir)then
          if(j.lt.ir)then
            if(ra(j).lt.ra(j+1))j=j+1
          endif
          if(rra.lt.ra(j))then
            ra(i)=ra(j)
            i=j
            j=j+j
          else
            j=ir+1
          endif
        goto 20
        endif
        ra(i)=rra
      goto 10
      END
