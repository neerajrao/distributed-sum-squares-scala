Distributed Sum of Squares using Scala Actors
===

This project was done in conjunction with Shyamala Athaide at UFL.

Here, we calculate the sum of squares of K numbers starting from [1,N]. The most time consuming operation here is calculating the squares of the numbers. Hence, a na?ve implementation that simply assigns number sequences to different actors would be very inefficient since the squares of numbers common to overlapping sequences would be calculated multiple times. As an example, consider a sequence length of K = 4. The first four sequences would be

<pre>
1,2,3,4
2,3,4,5
3,4,5,6
4,5,6,7
</pre>

We can see that the square of 2 would have to be calculated twice, the square of 3 thrice and the squares of the successive numbers (up to N-2) 4 times.

In order to avoid repetition, we use the fact that, in general, the sum of the squares of K numbers can be represented by

![sum of squares formula](images/ssq_formula.png?raw=true)

where M is the median of the sequence. This sum can be simplified to `K*M^2+2*C` where

![M and C formulas](images/M_n_C_formula.png?raw=true)

The advantages of this approach are that

1. Each sequence of K numbers is characterized by a unique median M and hence, to calculate the sum of each sequence, only this one number needs to be squared (i.e., we do not need to square the numbers common to the various sequences repeatedly)
2. C is the same for all sequences which means that it (and the squares that make it up) needs to be computed only once!

The constant term C and the range of medians M for a given K are shown below:

![C and M ranges](images/M_n_C_range.png?raw=true)

#### Architecture:

**Local**

![local architecture](images/local-architecture.png?raw=true)

**Remote**

![remote architecture](images/remote-architecture.png?raw=true)

#### Logic:

The top level class Boss is given the N and K values. There is one layer of actors beneath him. In this layer, we have W units of class CCalculator and U units of class MCalculator. W and U are determined experimentally as described in the next section.

Since C remains constant given a K, we start by calculating C. As can be seen from Table 1, each of the W units is given either 1, 2 etc. if K is odd or 0.5, 1.5 etc. if K is even. The W units work in parallel and square the numbers that they are given. The squares are returned to the Boss, who adds them up to get C (this exchange of numbers is shown by the arrows between the Boss and CCalculator units). Once C has been calculated, these W units are killed. Note that if W is smaller than the minimum number of units required `K/2`, the Boss reuses the units that have sent a response.

Once C has been calculated, we can proceed to get the sums of squares of the K-length sequences. This is done by the U units of class MCalculator. Each unit is given the median M and the value C that was calculated in the last step. Each unit then computes the sum of the following V sequences using `K*M^2+2*C` (where M increases by 1 for each successive sequence) and checks if the sum is a perfect square. If it is, the unit then prints the starting value of the sequence using the formula `(M-K/2).toInt + 1`. The arrows between MCalculator and Boss show that the Boss gives the starting median M for V consecutive sequences and the MCalculators respond asking for the next M when they?re done. Note that if U is smaller than the minimum number of units required `N/V`, the Boss reuses the units that have sent a response. Once all the medians have been dealt with, the Boss kills off the MCalculator units and then exits itself. 


#### Performance:

The performance at both the local at remote ends is shown in the graphs below:

**Local**
![local performance](images/local-performance.png?raw=true)

**Remote**
![remote performance](images/remote-performance.png?raw=true)

