package fraug.votes.backend.model

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.runBlocking
import org.mindrot.jbcrypt.BCrypt
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class RootQuery : Query {
  fun hello(): String {
    return "\uD83D\uDC38"
  }
}

@Component
class RootMutation : Mutation {
  /**
   * register a vote for the active question
   *
   * @param userId an identifier for a user. A given user can change their votes by voting multiple times
   * @param choiceId a choiceId. No validation is made. If an invalid choiceId is entered, the vote will
   * be ignored
   *
   * @return always returns true. Failures will be returned as errors
   */
  fun vote(userId: String, choiceId: String): Boolean {
    Firestore.upsertVote(userId, choiceId)
    return true
  }
}
