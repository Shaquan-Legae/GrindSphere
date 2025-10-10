package com.example.grindsphere.hustler

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object MessageBookingManager {
    private const val TAG = "MessageBookingManager"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Unified method to create booking AND conversation in one call
     */
    fun createBookingWithConversation(
        context: Context,
        serviceId: String,
        serviceName: String,
        hustlerId: String,
        customerMessage: String = "I'm interested in your service!",
        onSuccess: (bookingId: String, conversationId: String) -> Unit = { _, _ -> },
        onError: (Exception) -> Unit = {}
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Step 1: Get customer details
        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { customerDoc ->
                val customerName = customerDoc.getString("name") ?: "Customer"
                val customerEmail = customerDoc.getString("email") ?: ""

                // Step 2: Get hustler details
                firestore.collection("users").document(hustlerId).get()
                    .addOnSuccessListener { hustlerDoc ->
                        val hustlerName = hustlerDoc.getString("name") ?: "Service Provider"

                        // Step 3: Create booking data
                        val bookingData = hashMapOf(
                            "serviceId" to serviceId,
                            "serviceName" to serviceName,
                            "customerId" to currentUser.uid,
                            "customerName" to customerName,
                            "customerEmail" to customerEmail,
                            "hustlerId" to hustlerId,
                            "hustlerName" to hustlerName,
                            "status" to "pending",
                            "message" to customerMessage,
                            "timestamp" to Timestamp.now(),
                            "price" to 0.0 // You might want to get this from service
                        )

                        // Step 4: Create booking
                        firestore.collection("bookingRequests").add(bookingData)
                            .addOnSuccessListener { bookingDoc ->
                                val bookingId = bookingDoc.id
                                Log.d(TAG, "Booking created: $bookingId")

                                // Step 5: Create or get conversation
                                createOrGetConversation(
                                    participant1 = currentUser.uid,
                                    participant2 = hustlerId,
                                    participant1Name = customerName,
                                    participant2Name = hustlerName,
                                    serviceName = serviceName,
                                    bookingId = bookingId,
                                    onSuccess = { conversationId ->
                                        Log.d(TAG, "Conversation created: $conversationId")

                                        // Send initial message
                                        sendInitialMessage(
                                            conversationId = conversationId,
                                            senderId = currentUser.uid,
                                            senderName = customerName,
                                            message = customerMessage,
                                            bookingId = bookingId
                                        )

                                        onSuccess(bookingId, conversationId)
                                        Toast.makeText(context, "Connection request sent! You can now chat with the provider.", Toast.LENGTH_LONG).show()
                                    },
                                    onError = onError
                                )
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Booking creation failed: ${e.message}", e)
                                onError(e)
                                Toast.makeText(context, "Failed to send request: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get hustler details: ${e.message}", e)
                        onError(e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get customer details: ${e.message}", e)
                onError(e)
            }
    }

    /**
     * Smart conversation creation that prevents duplicates
     */
    private fun createOrGetConversation(
        participant1: String,
        participant2: String,
        participant1Name: String,
        participant2Name: String,
        serviceName: String,
        bookingId: String? = null,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        val participants = listOf(participant1, participant2).sorted()
        val participantNames = mapOf(
            participant1 to participant1Name,
            participant2 to participant2Name
        )

        // Look for existing conversation
        firestore.collection("conversations")
            .whereArrayContains("participants", participant1)
            .get()
            .addOnSuccessListener { snapshot ->
                val existingConvo = snapshot.documents.firstOrNull { doc ->
                    val convoParticipants = doc.get("participants") as? List<*>
                    convoParticipants?.containsAll(participants) == true
                }

                if (existingConvo != null) {
                    // Update existing conversation with latest info
                    val updates = hashMapOf<String, Any>(
                        "lastMessage" to "Booking: $serviceName",
                        "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                        "participantNames" to participantNames,
                        "serviceName" to serviceName
                    )
                    bookingId?.let { updates["bookingId"] = it }

                    existingConvo.reference.set(updates, SetOptions.merge())
                        .addOnSuccessListener {
                            onSuccess(existingConvo.id)
                        }
                        .addOnFailureListener { e ->
                            onError(e)
                        }
                } else {
                    // Create new conversation
                    val newConvo = hashMapOf(
                        "participants" to participants,
                        "participantNames" to participantNames,
                        "lastMessage" to "Booking: $serviceName",
                        "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                        "serviceName" to serviceName,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "bookingId" to bookingId
                    )

                    firestore.collection("conversations").add(newConvo)
                        .addOnSuccessListener { docRef ->
                            onSuccess(docRef.id)
                        }
                        .addOnFailureListener { e ->
                            onError(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    /**
     * Send initial message when booking is created
     */
    private fun sendInitialMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        message: String,
        bookingId: String? = null
    ) {
        val messageData = hashMapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to message,
            "timestamp" to Timestamp.now(),
            "type" to "booking_request",
            "bookingId" to bookingId
        )

        firestore.collection("conversations").document(conversationId)
            .collection("messages")
            .add(messageData)
    }

    /**
     * Accept booking and update conversation
     */
    fun acceptBooking(
        bookingId: String,
        conversationId: String? = null,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        // Update booking status
        firestore.collection("bookingRequests").document(bookingId)
            .update("status", "accepted")
            .addOnSuccessListener {
                // Update conversation if provided
                if (conversationId != null) {
                    val convId = conversationId
                    val messageData = hashMapOf(
                        "senderId" to "system",
                        "senderName" to "System",
                        "text" to "Booking accepted! You can now discuss the details.",
                        "timestamp" to Timestamp.now(),
                        "type" to "system"
                    )

                    firestore.collection("conversations").document(convId)
                        .collection("messages")
                        .add(messageData)

                    // Update conversation last message - FIXED: use mapOf() for multiple fields
                    firestore.collection("conversations").document(convId)
                        .update(
                            mapOf(
                                "lastMessage" to "Booking accepted",
                                "lastMessageTimestamp" to FieldValue.serverTimestamp()
                            )
                        )
                        .addOnSuccessListener {
                            Log.d(TAG, "Conversation updated successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to update conversation: ${e.message}")
                        }
                }

                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}