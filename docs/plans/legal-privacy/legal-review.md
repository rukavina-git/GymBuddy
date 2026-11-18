# Legal review: terms + privacy policy (AI, 2026-11-17)

Owner decision 2026-11-17: no paid lawyer (free portfolio app). Claude
reviewed `legal/terms.md`, `legal/privacy.md` and
`legal/delete-account.md` against Croatian/EU consumer and data
protection rules. Not legal advice; good enough for a free app with no
ads and no purchases. Re-run this review before paid features or ads.

## Verdicts

1. **Croatian language.** Croatian consumer law expects consumer
   information in Croatian. Risk for a free app with no payment: low.
   Left English only. Revisit with paid features.
2. **Identity and address.** Name, country and email are given. The
   E-Commerce rules ask for a geographic address for an information
   society service; for a private person with no revenue, enforcement
   risk is low. No home address published (owner privacy). If an obrt
   or d.o.o. is registered, add its registered address.
3. **Liability.** Unlimited for intent, gross negligence and injury;
   foreseeable-damage cap otherwise; consumer rights saved. That
   matches what Croatian law (ZOO) allows. Training disclaimer is the
   usual "not medical advice, own risk"; it can't excuse our own fault,
   and the terms don't claim that. OK.
4. **Digital content (2019/770).** Covered: free app paid with data.
   The "consumer rights stay" sentence keeps conformity rights; feature
   changes are announced and export is offered. OK.
5. **Consent withdrawal.** Delete account in the app (You → Delete
   account, one confirm) or by email. As easy as giving it. Now said in the policy.
6. **Legal bases.** 6(1)(b) account, 9(2)(a) health data, 6(1)(f)
   crash reports and logs. Crashlytics reading device info is a grey
   area under ePrivacy Art. 5(3); common practice for free apps, user
   id only when signed in, no health data. Accepted risk.
   Owner: no opt-out toggle; the policy says crash reports can't be
   turned off (don't use the app if you object). Art. 21 objections
   by email are still answered.
7. **Transfers.** DPF + SCCs via Google's and GitHub's standard DPAs,
   which apply through their terms. OK.
8. **Retention.** Server logs rotate by size (Docker 10 MB × 3), so no
   fixed period can be promised; "usually within a few weeks" is
   honest. Backups 30 days, Crashlytics 90 days. OK.
9. **Age.** 16+ matches the Croatian GDPR consent age; no gate, stated
   in terms, policy and Play audience. OK.
10. **Monetization.** Nothing blocks it. Before subscriptions: add
    price, renewal, cancellation, and the withdrawal-right waiver for
    digital content. Before ads: consent (CMP) and a policy update.
11. **Name.** "Gym Tracker" is descriptive, weak as a mark but low
    risk to use. OK.

## Changes made (privacy.md, 2026-11-17)

- Says the account is optional and what is required to create one
  (GDPR Art. 13(2)(e)).
- Says there is no profiling or automated decision-making
  (Art. 13(2)(f)).
- Consent withdrawal: in the app or by email.
- Crash reports: required, can't be turned off (owner).

Health-data consent wording unchanged, so no re-consent needed.

## Second pass (legal plugin compliance check, 2026-11-18)

Claims in the policy checked against the code: all match (export,
delete incl. Firebase user, consent title and version, Google picture
link, Crashlytics key allowlist, 30-day encrypted backups, 10 MB × 3
logs, licences screen).

Changed:
- Consent body says how to withdraw (Art. 7(3)); CONSENT_VERSION
  2026-11-18. Recorded only, so no re-consent.
- Account and welcome lines: you agree to the Terms only; the privacy
  policy is linked as information, not agreed to.
- privacy.md: GitHub backups described as encrypted before upload;
  DPA claim kept for netcup and Google only.

Owner to do:
- Sign netcup's AVV in the customer panel.
- Accept Google's data processing terms in Firebase project settings
  (privacy section), both projects.
- Keep a short Art. 30 record of processing (health data means the
  small-business exemption doesn't apply).
