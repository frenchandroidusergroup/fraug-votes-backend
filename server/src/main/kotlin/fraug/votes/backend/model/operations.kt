package fraug.votes.backend.model

import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asPublisher
import org.mindrot.jbcrypt.BCrypt
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class RootQuery : Query {
  fun test(): Int = 0
}

@Component
class RootMutation : Mutation {
  fun setup(password: String): Boolean {
    val existingPasswordHash = Firestore.getPasswordHash()
    check (existingPasswordHash == null) {
      "This instance has already been set up"
    }

    val hash = BCrypt.hashpw(password, BCrypt.gensalt())
    Firestore.setPasswordHash(hash)

    return true
  }

  fun admin(password: String): Admin {
    return Admin()
  }

  /**
   * register a vote for the active question
   *
   * @param userId an identifier for a user. A given user can change their votes by voting multiple times
   * @param choiceId a choiceId. No validation is made. If an invalide choiceId is entered, the vote will
   * be ignored
   *
   * @return always returns true. Failures will be returned as errors
   */
  fun vote(userId: String, choiceId: String): Boolean {
    Firestore.upsertVote(userId, choiceId)
    return true
  }
}


@Component
class RootSubscription : Subscription {
  @OptIn(FlowPreview::class)
  fun votes(questionId: String): Publisher<List<Vote>> {
    val choiceIds = Firestore.getQuestionChoices(questionId)

    check(choiceIds != null)

    return Firestore.listenToVotes(questionId).map { votes ->
      choiceIds.map { choiceId ->
        Vote(choiceId, votes.count { vote -> vote == choiceId })
      } + Vote(null, votes.count { vote -> !choiceIds.contains(vote)})
    }.debounce(100)
      .asPublisher()
  }
}

class Admin {
  fun deletePassword(): Boolean {
    Firestore.deletePasswordHash()
    return true
  }

  /**
   * Create a new question
   *
   * @param id a unique id for this question
   * @param choiceIds a list of choices. For a given question, all choices must be different
   * This is typically the identifier that users will input in the Twitch chat with `!benjamin`
   */
  fun createQuestion(questionId: String, choiceIds: List<String>): Boolean {
    Firestore.createQuestion(questionId, choiceIds)
    return true
  }

  /**
   * Activate the given question. Votes will use this question from now on
   *
   * @param questionId the question to activate or null to deactivate all questions
   *
   * @return always returns true. Failures will be returned as errors
   */
  fun activateQuestion(questionId: String?): Boolean {
    Firestore.setActiveQuestion(questionId)
    return true
  }

  /**
   * Delete the given question
   *
   * @return always returns true. Failures will be returned as errors
   */
  fun deleteQuestion(questionId: String): Boolean {
    Firestore.deleteQuestion(questionId)
    return true
  }
}

class Vote(val choiceId: String?, val count: Int)