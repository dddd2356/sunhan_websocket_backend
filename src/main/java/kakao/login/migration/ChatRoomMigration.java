package kakao.login.migration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import kakao.login.entity.ChatMessage;
import kakao.login.entity.ChatRoom;
import kakao.login.entity.EmployeeEntity;
import kakao.login.repository.ChatMessageRepository;
import kakao.login.repository.ChatRoomRepository;
import kakao.login.repository.EmployeeRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
@Profile("migration") // Only run when 'migration' profile is active
public class ChatRoomMigration implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;
    private final MongoClient mongoClient;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public ChatRoomMigration(
            MongoTemplate mongoTemplate,
            MongoClient mongoClient,
            ChatRoomRepository chatRoomRepository,
            ChatMessageRepository chatMessageRepository,
            EmployeeRepository employeeRepository) {
        this.mongoTemplate = mongoTemplate;
        this.mongoClient = mongoClient;
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("Starting chat room migration...");

        // Get MongoDB collections
        MongoDatabase database = mongoClient.getDatabase("your_db_name"); // replace with your MongoDB database name
        MongoCollection<Document> chatRoomsCollection = database.getCollection("chatRooms");
        MongoCollection<Document> messagesCollection = database.getCollection("chat_messages");

        // Map to store old MongoDB ID to new MySQL ID mapping
        Map<String, Long> roomIdMapping = new HashMap<>();

        // Migrate chat rooms
        for (Document doc : chatRoomsCollection.find()) {
            String oldId = doc.getObjectId("_id").toString();
            String name = doc.getString("name");
            String createdBy = doc.getString("createdBy");
            boolean isGroupChat = doc.getBoolean("isGroupChat", false);

            // Get timestamps
            Date createdAtDate = doc.getDate("createdAt");
            Date lastActivityDate = doc.getDate("lastActivity");

            LocalDateTime createdAt = createdAtDate != null ?
                    createdAtDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() :
                    LocalDateTime.now();

            LocalDateTime lastActivity = lastActivityDate != null ?
                    lastActivityDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() :
                    LocalDateTime.now();

            // Create new chat room entity
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setName(name);
            chatRoom.setCreatedBy(createdBy);
            chatRoom.setCreatedAt(createdAt);
            chatRoom.setLastActivity(lastActivity);
            chatRoom.setGroupChat(isGroupChat);

            // Get participants
            List<String> participantIds = (List<String>) doc.get("participants");
            if (participantIds != null) {
                for (String userId : participantIds) {
                    EmployeeEntity employee = employeeRepository.findByUser_UserId(userId);
                    if (employee != null) {
                        chatRoom.addParticipant(employee,false);
                    }
                }
            }

            // Save the chat room and store ID mapping
            ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
            roomIdMapping.put(oldId, savedRoom.getId());

            System.out.println("Migrated chat room: " + oldId + " -> " + savedRoom.getId());
        }

        // Update MongoDB messages with new room IDs
        for (Map.Entry<String, Long> entry : roomIdMapping.entrySet()) {
            String oldRoomId = entry.getKey();
            Long newRoomId = entry.getValue();

            // Find all messages for this room
            for (Document messageDoc : messagesCollection.find(Filters.eq("roomId", oldRoomId))) {
                String messageId = messageDoc.getObjectId("_id").toString();

                // Update the roomId in MongoDB
                Document update = new Document("$set", new Document("roomId", newRoomId.toString()));
                messagesCollection.updateOne(Filters.eq("_id", messageDoc.getObjectId("_id")), update);

                System.out.println("Updated message: " + messageId + " with new roomId: " + newRoomId);
            }
        }

        System.out.println("Chat room migration completed successfully!");
    }
}