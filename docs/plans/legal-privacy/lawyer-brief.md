# Lawyer brief: Gym Tracker terms + privacy policy

Prepared 2026-11-17 by Claude for the owner to send to a lawyer
(Croatian/EU consumer + data protection). Not legal advice; these are
the questions a lawyer should answer.

## What to review

- Terms: https://gymtracker.rukavina.app/terms (source `legal/terms.md`)
- Privacy policy: https://gymtracker.rukavina.app/privacy
  (`legal/privacy.md`)
- Account deletion page: https://gymtracker.rukavina.app/delete-account
- Play Data safety answers: `docs/plans/legal-privacy/data-safety.md`

## Facts

- Android workout log, free, no ads, no in-app purchases yet. Paid
  features or ads may come later.
- Made by a private person in Croatia (no company or obrt yet).
- Works without an account (data stays on the phone). With an account,
  workouts, body weight and profile are synced to a server in Germany
  (netcup). Health data is stored only after explicit consent on a
  separate screen (Art. 9(2)(a) GDPR), with version and time stored.
- Sign-in: Firebase Authentication (email or Google). Crash reports:
  Firebase Crashlytics (user id if signed in, no health data).
  Encrypted DB backups on GitHub (USA), kept 30 days.
- Users 16+, no age check. Sold in the EU via Google Play.
- Texts are in English only.

## Questions

1. **Croatian language.** Do the terms and privacy policy need a
   Croatian version for consumers in Croatia (Zakon o zaštiti
   potrošača)? Other EU languages?
2. **Identity and address.** Do the terms or policy need a postal
   address (E-Commerce Directive Art. 5, GDPR Art. 13(1)(a)), or is an
   email enough for a private person? Would registering an obrt or
   d.o.o. change anything we should do now?
3. **Liability clause.** Is "Liability" valid against consumers under
   Croatian law (ZOO, unfair terms rules)? Does the training
   disclaimer ("train at your own risk", see a doctor and a trainer)
   do what it can, or is anything in it void or missing?
4. **Digital Content Directive (2019/770).** The app is free but the
   user gives personal data. Does "we can't promise the app is always
   available or free of errors" and "we may change or stop features"
   fit the conformity and modification rules? Is the "consumer rights
   stay" sentence enough?
5. **Consent withdrawal.** The only way to withdraw health-data consent
   is to delete the account (in the app, or by email). Is that "as
   easy as giving it" (GDPR Art. 7(3)), or do we need a separate
   "stop syncing, delete server copy, keep account" option?
6. **Legal bases.** Account data under Art. 6(1)(b), Crashlytics and
   server logs under Art. 6(1)(f). OK? Does Crashlytics need consent
   under ePrivacy / ZEK (Croatian implementation), since it reads
   device info?
7. **Transfers.** Google and GitHub (USA) rely on DPF + SCCs. Is the
   wording enough? Do we need to sign anything beyond their standard
   DPAs?
8. **Retention.** "Server logs are overwritten usually within a few
   weeks." Do we need a fixed period?
9. **Age.** 16+ with no age check, Play audience 16-17 and 18+. Enough?
10. **Later monetization.** What must change in the terms before
    subscriptions or ads (withdrawal right for digital content, price
    changes, ad consent/CMP)? Anything in the current texts that would
    make that harder?
11. **Name.** Public name "Gym Tracker" (EU mark GYMBUDDY exists, so
    the old name was dropped). Any risk with "Gym Tracker" as a
    generic name?

## What we want back

Marked-up `terms.md` and `privacy.md` (or a list of changes), answers
to the questions above, and a yes/no on "fine to release free on Play
in the EU".
