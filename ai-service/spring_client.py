"""
Service-to-Service REST client connecting AI Service to Spring Boot backend.
Retrieves live loan application status, document lists, and audit updates
with customer authorization isolation.
"""

import httpx
import logging
from typing import Optional, Dict, Any, List
from config import settings

logger = logging.getLogger("BankCore360.AI.SpringClient")

class SpringBootClient:
    def __init__(self):
        self.base_url = settings.SPRING_BOOT_URL
        self.service_token = settings.SPRING_BOOT_SERVICE_TOKEN

    async def get_customer_applications(self, customer_username: str) -> List[Dict[str, Any]]:
        """
        Fetches the customer's real loan applications from Spring Boot.
        """
        if not customer_username:
            return []

        url = f"{self.base_url}/api/loans/my-applications"
        # We pass the customer username in headers for authenticated service call
        headers = {
            "X-Internal-Service-Token": self.service_token,
            "X-Authenticated-User": customer_username,
            "Accept": "application/json"
        }

        try:
            async with httpx.AsyncClient(timeout=4.0) as client:
                res = await client.get(url, headers=headers)
                if res.status_code == 200:
                    return res.json()
        except Exception as e:
            logger.warning(f"Could not reach Spring Boot at {url}: {e}")

        return []

    async def get_application_by_reference(self, reference: str, customer_username: str) -> Optional[Dict[str, Any]]:
        """
        Fetches a specific application and enforces ownership:
        customer can only view their own application.
        """
        apps = await self.get_customer_applications(customer_username)
        for app in apps:
            if app.get("applicationReference") == reference:
                return app
        return None

    async def get_application_documents(self, reference: str, customer_username: str) -> List[Dict[str, Any]]:
        url = f"{self.base_url}/api/loans/{reference}/documents"
        headers = {
            "X-Internal-Service-Token": self.service_token,
            "X-Authenticated-User": customer_username,
            "Accept": "application/json"
        }

        try:
            async with httpx.AsyncClient(timeout=4.0) as client:
                res = await client.get(url, headers=headers)
                if res.status_code == 200:
                    return res.json()
        except Exception as e:
            logger.warning(f"Could not fetch documents for {reference}: {e}")

        return []

    async def get_status_summary(self, customer_username: str, reference: Optional[str] = None) -> Dict[str, Any]:
        """
        Builds a comprehensive application status report for customer assistant.
        """
        if not customer_username:
            return {
                "hasApplications": False,
                "message": "Please log in to your BankCore360 customer account to check your loan application status."
            }

        apps = await self.get_customer_applications(customer_username)
        if not apps:
            return {
                "hasApplications": False,
                "message": f"No loan applications were found for account '{customer_username}'. You can apply online via the Customer Portal."
            }

        # Select target application
        target_app = None
        if reference:
            for a in apps:
                if a.get("applicationReference") == reference:
                    target_app = a
                    break

        if not target_app:
            target_app = apps[0]  # Most recent application

        ref = target_app.get("applicationReference", "UNKNOWN")
        status = target_app.get("status", "UNKNOWN")
        loan_type = target_app.get("loanType", "PERSONAL")
        amt = target_app.get("requestedAmount", 0.0)

        docs = await self.get_application_documents(ref, customer_username)
        pending_docs = [d.get("documentType") for d in docs if d.get("documentStatus") == "PENDING"]
        rejected_docs = [
            {"type": d.get("documentType"), "reason": d.get("rejectionReason", "Not specified")}
            for d in docs if d.get("documentStatus") == "REJECTED"
        ]

        # Determine next steps based on real state
        if status == "SUBMITTED":
            if not docs:
                next_step = "Upload mandatory PAN, Address Proof, and Income documents in the Customer Portal."
                explanation = "Your loan application has been submitted, but no supporting documents have been uploaded yet."
            elif pending_docs:
                next_step = f"Wait for Bank Checker verification of your pending documents: {', '.join(pending_docs)}."
                explanation = f"Your application is queued for Checker document inspection."
            else:
                next_step = "Application is being reviewed by the credit desk."
                explanation = "Your documents are in review."
        elif status == "UNDER_REVIEW":
            next_step = "Checker official is actively reviewing your uploaded documents and credit profile."
            explanation = "Your application is currently in Maker-Checker verification."
        elif status == "PENDING_MANAGER_APPROVAL":
            next_step = "All documents have been verified by Checker! Awaiting Branch Manager final sanction and pricing determination."
            explanation = "Your application has passed document verification and has reached Manager Approval stage."
        elif status == "APPROVED":
            next_step = "Your loan is approved! Awaiting automated disbursement to your BankCore360 account."
            explanation = f"Congratulations! Your {loan_type} loan was APPROVED for ₹{target_app.get('approvedAmount', amt):,.2f} at {target_app.get('interestRate', 0)}% p.a."
        elif status == "DISBURSED":
            next_step = "Review your auto-generated monthly EMI amortization schedule in 'My Applications'."
            explanation = f"Your loan has been disbursed! Disbursement Reference: {target_app.get('disbursementReference', 'N/A')}."
        elif status == "REJECTED":
            reason = target_app.get("rejectionReason", "Credit policy criteria not met")
            next_step = "Review the rejection reason and discuss re-application options with your branch officer."
            explanation = f"Your loan application was not approved. Official Reason: {reason}."
        else:
            next_step = "Check your customer dashboard for recent notifications."
            explanation = f"Current application status is {status}."

        return {
            "hasApplications": True,
            "applicationReference": ref,
            "status": status,
            "loanType": loan_type,
            "requestedAmount": amt,
            "pendingDocuments": pending_docs,
            "rejectedDocuments": rejected_docs,
            "nextStep": next_step,
            "explanation": explanation
        }

spring_client = SpringBootClient()

