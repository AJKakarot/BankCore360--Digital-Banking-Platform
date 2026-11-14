"""
Service-to-Service REST client connecting AI Service to Spring Boot backend.
Retrieves live accounts, double-entry ledger transactions, spending summaries,
fraud risk metadata, and loan products with strict customer authorization isolation.
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

    def _get_headers(self, customer_username: Optional[str] = None) -> Dict[str, str]:
        headers = {
            "X-Internal-Service-Token": self.service_token,
            "Accept": "application/json"
        }
        if customer_username:
            headers["X-Authenticated-User"] = customer_username
        return headers

    async def get_customer_accounts(self, customer_username: str) -> List[Dict[str, Any]]:
        if not customer_username:
            return []
        url = f"{self.base_url}/api/internal/customer/accounts"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                res = await client.get(url, headers=self._get_headers(customer_username))
                if res.status_code == 200:
                    return res.json()
        except Exception as e:
            logger.warning(f"Failed to fetch accounts from {url}: {e}")
        return []

    async def get_customer_spending_summary(self, customer_username: str) -> Optional[Dict[str, Any]]:
        if not customer_username:
            return None
        url = f"{self.base_url}/api/internal/customer/spending-summary"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                res = await client.get(url, headers=self._get_headers(customer_username))
                if res.status_code == 200:
                    return res.json()
        except Exception as e:
            logger.warning(f"Failed to fetch spending summary from {url}: {e}")
        return None

    async def get_customer_transactions(self, customer_username: str) -> List[Dict[str, Any]]:
        if not customer_username:
            return []
        url = f"{self.base_url}/api/internal/customer/transactions"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                res = await client.get(url, headers=self._get_headers(customer_username))
                if res.status_code == 200:
                    return res.json()
        except Exception as e:
            logger.warning(f"Failed to fetch transactions from {url}: {e}")
        return []

    async def get_transaction_detail(self, customer_username: str, ref_or_id: str) -> Optional[Dict[str, Any]]:
        if not customer_username or not ref_or_id:
            return None
        url = f"{self.base_url}/api/internal/customer/transactions/{ref_or_id}"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                res = await client.get(url, headers=self._get_headers(customer_username))
                if res.status_code == 200:
                    return res.json()
                elif res.status_code == 403:
                    logger.warning(f"Ownership check failed for transaction {ref_or_id}")
                    return {"error": "ACCESS_DENIED", "message": f"You do not have authorization to view transaction {ref_or_id}."}
        except Exception as e:
            logger.warning(f"Failed to fetch transaction detail from {url}: {e}")
        return None

    async def get_customer_fraud_metadata(self, customer_username: str) -> Optional[Dict[str, Any]]:
        if not customer_username:
            return None
        url = f"{self.base_url}/api/internal/customer/fraud-metadata"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                res = await client.get(url, headers=self._get_headers(customer_username))
                if res.status_code == 200:
                    return res.json()
        except Exception as e:
            logger.warning(f"Failed to fetch fraud metadata from {url}: {e}")
        return None

    async def get_loan_products(self) -> List[Dict[str, Any]]:
        url = f"{self.base_url}/api/internal/loans/products"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                res = await client.get(url, headers=self._get_headers())
                if res.status_code == 200:
                    return res.json()
        except Exception as e:
            logger.warning(f"Failed to fetch loan products from {url}: {e}")
        return []

    async def get_customer_applications(self, customer_username: str) -> List[Dict[str, Any]]:
        if not customer_username:
            return []
        url = f"{self.base_url}/api/loans/my-applications"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                res = await client.get(url, headers=self._get_headers(customer_username))
                if res.status_code == 200:
                    return res.json()
        except Exception as e:
            logger.warning(f"Could not reach Spring Boot at {url}: {e}")
        return []

    async def get_application_by_reference(self, reference: str, customer_username: str) -> Optional[Dict[str, Any]]:
        apps = await self.get_customer_applications(customer_username)
        for app in apps:
            if app.get("applicationReference") == reference:
                return app
        return None

    async def get_application_documents(self, reference: str, customer_username: str) -> List[Dict[str, Any]]:
        url = f"{self.base_url}/api/loans/{reference}/documents"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                res = await client.get(url, headers=self._get_headers(customer_username))
                if res.status_code == 200:
                    return res.json()
        except Exception as e:
            logger.warning(f"Could not fetch documents for {reference}: {e}")
        return []

    async def get_status_summary(self, customer_username: str, reference: Optional[str] = None) -> Dict[str, Any]:
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

        target_app = None
        if reference:
            for a in apps:
                if a.get("applicationReference") == reference:
                    target_app = a
                    break

        if not target_app:
            target_app = apps[0]

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

        if status == "SUBMITTED":
            next_step = "Upload mandatory PAN, Address Proof, and Income documents in the Customer Portal." if not docs else f"Wait for Checker verification of pending documents: {', '.join(pending_docs)}."
            explanation = "Your loan application has been submitted and is in initial intake."
        elif status == "UNDER_REVIEW":
            next_step = "Checker official is reviewing your uploaded documents and credit profile."
            explanation = "Your application is currently in Maker-Checker verification."
        elif status == "PENDING_MANAGER_APPROVAL":
            next_step = "All documents have been verified by Checker! Awaiting Branch Manager final sanction."
            explanation = "Your application has passed document verification and reached Manager Approval stage."
        elif status == "APPROVED":
            next_step = "Your loan is approved! Awaiting automated disbursement to your account."
            explanation = f"Congratulations! Your {loan_type} loan was APPROVED for ₹{target_app.get('approvedAmount', amt):,.2f}."
        elif status == "DISBURSED":
            next_step = "Review your monthly EMI amortization schedule in 'My Applications'."
            explanation = f"Your loan has been disbursed! Disbursement Reference: {target_app.get('disbursementReference', 'N/A')}."
        elif status == "REJECTED":
            reason = target_app.get("rejectionReason", "Credit policy criteria not met")
            next_step = "Review the rejection reason with your branch officer."
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
