/* The Minotaur’s birthday party was a success. The Minotaur received a lot of presents
from his guests. The next day he decided to sort all of his presents and start writing
“Thank you” cards. Every present had a tag with a unique number that was associated
with the guest who gave it. Initially all of the presents were thrown into a large bag with
no particular order. 

The Minotaur wanted to take the presents from this unordered bag
and create a chain of presents hooked to each other with special links (similar to storing
elements in a linked-list). In this chain (linked-list) all of the presents had to be ordered
according to their tag numbers in increasing order. The Minotaur asked 4 of his servants
to help him with creating the chain of presents and writing the cards to his guests. 

Each servant would do one of three actions in no particular order: 
1. Take a present from the unordered bag and add it to the chain in the correct location 
by hooking it to the predecessor’s link. The servant also had to make sure that the newly added present is
also linked with the next present in the chain. 

2. Write a “Thank you” card to a guest and remove the present from the chain. 
To do so, a servant had to unlink the gift from its
predecessor and make sure to connect the predecessor’s link with the next gift in the chain. 

3. Per the Minotaur’s request, check whether a gift with a particular tag was
present in the chain or not; without adding or removing a new gift, a servant would scan
through the chain and check whether a gift with a particular tag is already added to the
ordered chain of gifts or not. 

As the Minotaur was impatient to get this task done
quickly, he instructed his servants not to wait until all of the presents from the
unordered bag are placed in the chain of linked and ordered presents. Instead, every
servant was asked to alternate adding gifts to the ordered chain and writing “Thank you”
cards. The servants were asked not to stop or even take a break until the task of writing
cards to all of the Minotaur’s guests was complete. After spending an entire day on this
task the bag of unordered presents and the chain of ordered presents were both finally
empty! Unfortunately, the servants realized at the end of the day that they had more
presents than “Thank you” notes. What could have gone wrong? Can we help the
Minotaur and his servants improve their strategy for writing “Thank you” notes? Design
and implement a concurrent linked-list that can help the Minotaur’s 4 servants with this
task. In your test, simulate this concurrent “Thank you” card writing scenario by
dedicating 1 thread per servant and assuming that the Minotaur received 500,000
presents from his guests. */

import java.lang.Math;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashSet;
import java.util.Random;

public class BirthdayPresents implements Runnable {

    // Amount of presents
    // Should work with up to Integer.MAX_VALUE
    final static int numPresents = 500000;

    // Amount of servants
    final static int numServants = 4;
    final static Random rand = new Random();

    // HashSet that holds the unordered/unsorted bag of presents
    // size = amount presents left to sort and put in present chain
    static HashSet<Present> unorderedBag = new HashSet<Present>();
    static ReentrantLock unorderedBagLock = new ReentrantLock();

    // LinkedList that holds the present chain
    static PresentLinkedList linkedList = new PresentLinkedList();

    // HashSet that holds the thank you notes for all presents saying 
    //      ("Thank you for my present #" + present.tagNumber + "!")
    // size = amount of presents sorted in present chain, taken out, 
    //      and have thank you notes written for them
    static HashSet<String> thankYouNotes = new HashSet<String>();

    // variables for each thread
    private int threadNum;

    BirthdayPresents(int threadNum) {
        this.threadNum = threadNum;
    }

    public void run() {
        print("Thread #" + this.threadNum + " ready to go!");
        while (thankYouNotes.size() < numPresents) {
            // Returns random number in between 1 and 3, inclusive
            int action = rand.nextInt(3)+1;
            // int action = 1;
            print("Thread #" + this.threadNum + " - Action: " + action);
            print("Thread #" + this.threadNum + ": unordered bag size: " + unorderedBag.size());
            if (action == 1 && !unorderedBag.isEmpty()) {
                // Check a present out from the unordered bag and into the present chain
                unorderedBagLock.lock();
                try {
                    for (Present iter : unorderedBag) {
                        // Verify that no other lock is attempting to do the same at exact same time
                        if (iter.PutInPresentChain.get() == false) {
                            print("Thread #" + this.threadNum + ": tag #" + iter.tagNumber + " is able to be accessed (shouldnt be by other threads)!");
                            iter.PutInPresentChain.set(true);
                            // Quickly remove current present from unordered bag
                            unorderedBag.remove(iter);

                            // Place bag in present chain
                            // Potentially lock the chain so only this thread can put something in, 
                            //      to prevent things being placed in at the exact same time (but unlikely)

                            // Lock the linkedList so no other thread messes it up while this servant looks
                            print("Thread #" + this.threadNum + ": locking the present chain so no other thread can access it");
                            linkedList.deadlock.lock();
                            try {
                                Present tempHead = linkedList.head;
                                print("Thread #" + this.threadNum + ": printing present chain BEFORE");
                                printPresentChain();
                                // Check if head is null (linkedList is empty)
                                if (linkedList.head == null) {
                                    print("Thread #" + this.threadNum + ": Present chain is empty so creating new linkedlist");
                                    // Start a new linkedList
                                    linkedList.head = iter;
                                    linkedList.count++;
                                } else if (linkedList.head.next == null) {
                                    // Decide if current tag goes behind or in front
                                    if (linkedList.head.tagNumber > iter.tagNumber) {
                                        // iter goes in front
                                        print("Thread #" + this.threadNum + ": Present chain starts with a bigger number so inserting current tag as the head");
                                        iter.next = linkedList.head;
                                        linkedList.head = iter;
                                        linkedList.count++;
                                    } else {
                                        // iter goes behind
                                        print("Thread #" + this.threadNum + ": Present chain starts with a smaller number so inserting current tag after the head");
                                        linkedList.head.next = iter;
                                        linkedList.count++;
                                    }
                                } else if (linkedList.head.tagNumber > iter.tagNumber) {
                                    // iter goes in front
                                    print("Thread #" + this.threadNum + ": Present chain starts with a bigger number so inserting current tag as the head (despite there being other presents in the chain)");
                                    iter.next = linkedList.head;
                                    linkedList.head = iter;
                                    linkedList.count++;
                                } else {
                                    print("Thread #" + this.threadNum + ": looping through present chain");
                                    while (tempHead.next != null) {
                                        // Loop rightward through linkedlist until tempHead.next = null or tempHead.next.tagNumber > iter.tagNumber
                                        if (tempHead.next.tagNumber > iter.tagNumber) {
                                            break;
                                        }
                                        tempHead = tempHead.next;
                                    }
                                    print("Thread #" + this.threadNum + ": Inserting current tag now in sorted order!");
                                    // Iter will now have next = tempHead.next, be linked as tempHead.next, be linked as tempHead.next.prev, and have tempHead as prev
                                    iter.next = tempHead.next;
                                    tempHead.next = iter;

                                    linkedList.count++;
                                }
                            } finally {
                                print("Thread #" + this.threadNum + ": printing present chain AFTER");
                                printPresentChain();
                                print("Thread #" + this.threadNum + ": unlocking the present chain so NOW other thread can access it");
                                linkedList.deadlock.unlock();
                            }
                            break;
                        }
                    }
                } finally {
                    unorderedBagLock.unlock();
                }
                
            } else if (action == 2) {
                linkedList.deadlock.lock();
                try {
                    // Write a thank you note for a present in the present chain
                    if (linkedList.head == null) {
                        print("Thread #" + this.threadNum + ": Nothing in present chain, thus continuing.");
                        continue;
                    }

                    // Delete the head
                    if (linkedList.head.next == null) {
                        if (linkedList.head.thankYouNotedGenerated.get() == false) {
                            linkedList.head.thankYouNotedGenerated.set(true);
                            Present tempPresent = linkedList.head;
                            linkedList.head = null;
                            createThankYouNote(tempPresent.tagNumber);
                            print("Thread #" + this.threadNum + ": Thank you note generated for tag #" + tempPresent.tagNumber);
                            linkedList.count--;
                        }
                        continue;
                    // Deleting any node after the head
                    } else {
                        // Loop through the present chain and find the first present that is not marked with a thank you note
                        Present tempPrev = linkedList.head;
                        Present tempHead = linkedList.head.next;
                        // Loop through linkedlist
                        while (tempHead != null) {
                            if (tempHead.thankYouNotedGenerated.get() == false) {
                                linkedList.head.thankYouNotedGenerated.set(true);
                                tempPrev.next = tempHead.next;
                                createThankYouNote(tempHead.tagNumber);
                                print(threadNum + ": " + ": Thank you note generated for tag #" + tempHead.tagNumber);
                                linkedList.count--;
                                break;
                            }
                            tempPrev = tempHead;
                            tempHead = tempHead.next;
                        }
                    }
                } finally {
                    linkedList.deadlock.unlock();
                }

            } else if (action == 3) {
                // Call minotaursRequest() and search a specific present for Minotaur-sama
                int desiredTagNum = minotaursRequest();
                boolean tagNumFound = false;
                linkedList.deadlock.lock();
                try {
                    Present tempHead = linkedList.head;
                    if (tempHead == null) {
                        print("Thread #" + this.threadNum + ": Item not in present because present chain is empty");
                    } else {
                        while (tempHead.next != null) {
                            if (tempHead.next.tagNumber == desiredTagNum) {
                                print("Thread #" + this.threadNum + ": desired tag number of " + desiredTagNum + " has been found!");
                                tagNumFound = true;
                                break;
                            }
                        }

                        if (tagNumFound == false) {
                            print("Thread #" + this.threadNum + ": desired tag number of " + desiredTagNum + " could not be found.");
                        }
                    }
                } finally {
                    linkedList.deadlock.unlock();
                }
            }
        }

        print("Thread #" + this.threadNum + " has stopped since "
            + "thank you note count has met goal!");
    }

    public static void main(String args[]) throws InterruptedException, IOException {
        prepareUnorderedBag();
        Runnable[] servants = new BirthdayPresents[numServants];
        for (int i = 0; i < numServants; i++) {
            servants[i] = new BirthdayPresents(i);
            new Thread(servants[i]).start();
        }
    }

    private static void printPresentChain() {
        Present tempHead = linkedList.head;
        System.out.println("***Present Chain of size("+ linkedList.count +"): ");
        while (tempHead != null) {
            System.out.print(tempHead.tagNumber + "->");
            tempHead = tempHead.next;
        }
        System.out.println();
    }

    private static void prepareUnorderedBag() {
        print("**Unordered Bag:");
        for (int i = 0; i < numPresents; i++) {
            Present tempPresent = new Present(i+1);
            unorderedBag.add(tempPresent);
            print(tempPresent.tagNumber);
        }

        print("The unordered bag of " + numPresents + " presents has now been prepared.");
    }

    // Helper method for printing without needing to type System.out.println in its entirety
    private static void print(Object s) {
        try {
            System.out.println(s);
        } catch (Exception e) {
            System.out.println(s.getClass() + " object cannot be printed");
        }
    }

    private static void createThankYouNote(int tagNumber) {
        thankYouNotes.add(("Thank you for present #" + tagNumber + "!"));
        print("Printing all thank you notes:");
        for (String iter: thankYouNotes) {
            print(iter);
        }
    }

    private static int minotaursRequest() {
        // Returns random number in between 1 and numPresents, inclusive
        return rand.nextInt(numPresents)+1;
    }
}

// Simulating the present chain for all numerically tagged present
public class PresentLinkedList {
    public ReentrantLock deadlock;
    public Present head;
    public int count;
    PresentLinkedList() {
        deadlock = new ReentrantLock();
        head = null;
        count = 0;
    }
    // Potentially a lock the chain so only one thread can put something in, 
    //      to prevent things being placed in at the exact same time (but unlikely)
}

public class Present {
    public int tagNumber;
    /* Defines whether the present was taken out of the unordered bag */
    public AtomicBoolean PutInPresentChain;
    /* Defines whether the present was taken out of the present chain */
    public AtomicBoolean thankYouNotedGenerated;
    public Present prev;
    public Present next;

    Present(int tagNumber) {
        this.tagNumber = tagNumber;
        PutInPresentChain = new AtomicBoolean(false);
        thankYouNotedGenerated = new AtomicBoolean(false);
        prev = null;
        next = null;;
    }
}