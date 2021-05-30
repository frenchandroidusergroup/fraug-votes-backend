package fraug.votes.backend.model

import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.FirestoreOptions
import org.mindrot.jbcrypt.BCrypt
import org.springframework.stereotype.Component


private val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
  .setProjectId("fraug-votes")
  .setCredentials(GoogleCredentials.getApplicationDefault())
  .build();
private val firestoreDb = firestoreOptions.service

@Component
class RootQuery : Query {
  fun test(): Int = 0
}

@Component
class RootMutation : Mutation {
  fun setup(password: String): Boolean {
    val documentRef = firestoreDb.document("settings")
    val existingPassword = documentRef.get().get().getString("password")
    check (existingPassword == null) {
      "This instance has already been setup"
    }

    val hash = BCrypt.hashpw(password, BCrypt.gensalt())
    documentRef.set(mapOf("password" to password)).get()

    return true
  }
}