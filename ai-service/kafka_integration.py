"""
Kafka Event Producer and Consumer for BankCore360 AI Service.
Integrates AI policy updates, document extraction, and assistance logs
with error handling and graceful offline fallback.
"""

import json
import uuid
import logging
from datetime import datetime, timezone
from typing import Dict, Any, Optional
from config import settings

logger = logging.getLogger("BankCore360.AI.Kafka")

TOPIC_POLICY_UPDATED = "loan-policy-updated"
TOPIC_EXTRACTION_COMPLETED = "document-extraction-completed"
TOPIC_AI_REQUESTED = "ai-assistance-requested"

class KafkaService:
    def __init__(self):
        self.enabled = settings.KAFKA_ENABLED
        self.bootstrap_servers = settings.KAFKA_BOOTSTRAP_SERVERS
        self._producer = None

        if self.enabled:
            self._init_producer()

    def _init_producer(self):
        try:
            from kafka import KafkaProducer
            self._producer = KafkaProducer(
                bootstrap_servers=self.bootstrap_servers.split(","),
                value_serializer=lambda v: json.dumps(v, default=str).encode("utf-8"),
                key_serializer=lambda k: k.encode("utf-8") if k else None,
                retries=3,
                request_timeout_ms=3000
            )
            logger.info(f"Kafka producer connected to {self.bootstrap_servers}")
        except Exception as e:
            logger.info(f"Kafka unavailable or not running ({e}). Running in decoupled mode.")
            self._producer = None

    def publish_event(self, topic: str, event_type: str, aggregate_id: str, payload: Dict[str, Any]) -> str:
        event_id = str(uuid.uuid4())
        event = {
            "eventId": event_id,
            "eventType": event_type,
            "aggregateId": aggregate_id,
            "occurredAt": datetime.now(timezone.utc).isoformat(),
            "version": 1,
            "payload": payload
        }

        if self._producer:
            try:
                self._producer.send(topic, key=aggregate_id, value=event)
                logger.info(f"Published {event_type} to topic '{topic}' (Event ID: {event_id})")
            except Exception as e:
                logger.warning(f"Failed publishing to Kafka topic '{topic}': {e}")
        else:
            logger.debug(f"[Decoupled] Kafka event {event_type} generated: {event_id}")

        return event_id

    def emit_policy_updated(self, doc_id: int, title: str, category: str, version: str, action: str):
        self.publish_event(
            topic=TOPIC_POLICY_UPDATED,
            event_type="LOAN_POLICY_UPDATED",
            aggregate_id=str(doc_id),
            payload={
                "documentId": doc_id,
                "title": title,
                "category": category,
                "version": version,
                "action": action,
                "timestamp": datetime.now(timezone.utc).isoformat()
            }
        )

    def emit_document_extraction_completed(self, doc_type: str, applicant_name: Optional[str],
                                          is_valid: bool, overall_confidence: float):
        self.publish_event(
            topic=TOPIC_EXTRACTION_COMPLETED,
            event_type="DOCUMENT_EXTRACTION_COMPLETED",
            aggregate_id=doc_type,
            payload={
                "documentType": doc_type,
                "applicantName": applicant_name,
                "isSyntheticallyValid": is_valid,
                "overallConfidence": overall_confidence,
                "timestamp": datetime.now(timezone.utc).isoformat()
            }
        )

    def emit_ai_assistance_requested(self, intent: str, user: Optional[str], query_preview: str):
        self.publish_event(
            topic=TOPIC_AI_REQUESTED,
            event_type="AI_ASSISTANCE_REQUESTED",
            aggregate_id=user or "ANONYMOUS",
            payload={
                "intent": intent,
                "user": user,
                "queryPreview": query_preview[:80],
                "timestamp": datetime.now(timezone.utc).isoformat()
            }
        )

kafka_service = KafkaService()
