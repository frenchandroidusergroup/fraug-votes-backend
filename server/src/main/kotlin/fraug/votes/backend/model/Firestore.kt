package fraug.votes.backend.model

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow


object Firestore {
    private val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
        .setProjectId("fraug-votes")
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build();
    private val firestoreDb = firestoreOptions.service

    fun getPasswordHash(): String? {
        val documentRef = firestoreDb.document("settings/admin")
        return documentRef.get().get().getString("password")
    }

    fun setPasswordHash(passwordHash: String) {
        val documentRef = firestoreDb.document("settings/admin")
        documentRef.set(mapOf("password" to passwordHash)).get()
    }

    fun deletePasswordHash() {
        val documentRef = firestoreDb.document("settings/admin")
        documentRef.delete()
    }

    fun createQuestion(questionId: String, choiceIds: List<String>) = withTransaction {
        val documentRef = firestoreDb.document("question/$questionId")

        val existingChoiceIds = it.get(documentRef).get().get("choiceIds")
        check(existingChoiceIds == null) {
            "There is already a question with id '$questionId'"
        }
        it.set(documentRef, mapOf("choiceIds" to choiceIds))
    }

    fun getQuestionChoices(questionId: String): List<String>? {
        val documentRef = firestoreDb.document("question/$questionId")

        return documentRef.get().get().get("choiceIds") as List<String>?
    }


    fun deleteQuestion(questionId: String) {
        val documentRef = firestoreDb.document("question/$questionId")
        deleteDocument(documentRef)
    }

    fun setActiveQuestion(questionId: String?) {
        val documentRef = firestoreDb.document("settings/admin")
        documentRef.update(mapOf("activeQuestion" to questionId))
    }

    fun upsertVote(userId: String, choiceId: String) = withTransaction {
        val adminDocumentRef = firestoreDb.document("settings/admin")
        val questionId = it.get(adminDocumentRef).get().get("activeQuestion")
        check (questionId != null) {
            "No active question"
        }
        val questionDocumentRef = firestoreDb.document("question/$questionId/vote/$userId")
        it.set(questionDocumentRef, mapOf("choiceId" to choiceId))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun listenToVotes(questionId: String) = callbackFlow {
        val registration = firestoreDb.collection("question/$questionId/vote").addSnapshotListener { querySnapshot, error ->
            if (querySnapshot != null) {
                val choices = querySnapshot.documents.map {
                    it.getString("choiceId")
                }

                kotlin.runCatching {
                    sendBlocking(choices)
                }
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    private fun deleteDocument(documentRef: DocumentReference) {
        documentRef.listCollections().forEach {
            it.listDocuments().forEach {
                deleteDocument(it)
            }
        }
        documentRef.delete().get()
    }

    private fun withTransaction(block: (Transaction) -> Unit)  = firestoreDb.runTransaction { block(it) }.get()
}

