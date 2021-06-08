package fraug.votes.backend.model

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.Transaction


object Firestore {
    private val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
        .setProjectId("fraug-votes")
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build();
    private val firestoreDb = firestoreOptions.service

    fun upsertVote(userId: String, choiceId: String) = withTransaction {
        val currentGame = it.get(firestoreDb.document("settings/admin")).get().get("currentGame")
        check (currentGame != null) {
            "No active currentGame"
        }
        val currentQuestion = it.get(firestoreDb.document("games/$currentGame")).get().get("currentQuestion")
        check (currentQuestion != null) {
            "No active question"
        }

        val questionDocumentReference = firestoreDb.document("games/$currentGame/questions/$currentQuestion")
        check (it.get(questionDocumentReference).get().get("voteOpened") == true) {
            "Votes are not open"
        }

        val votesDocumentReference = firestoreDb.document("games/$currentGame/questions/$currentQuestion/votes/$userId")
        it.set(votesDocumentReference, mapOf("vote" to choiceId))
    }

    private fun withTransaction(block: (Transaction) -> Unit)  = firestoreDb.runTransaction { block(it) }.get()
}

