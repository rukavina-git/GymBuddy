// Feedback admin page. Plain ES module, no build step. Talks to
// /v1/admin/** with the signed-in user's Firebase ID token; the backend
// decides who is an admin (ADMIN_UIDS). User text is only ever set with
// textContent, never innerHTML.
import { initializeApp } from "https://www.gstatic.com/firebasejs/13.3.0/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithPopup,
  signOut,
} from "https://www.gstatic.com/firebasejs/13.3.0/firebase-auth.js";

const STATUSES = [
  { key: "open", label: "Open" },
  { key: "solved", label: "Solved" },
  { key: "wont_fix", label: "Won't fix" },
  { key: "spam", label: "Spam" },
];
const PAGE_SIZE = 50;
const dateFormat = new Intl.DateTimeFormat("en-GB", {
  timeZone: "Europe/Zagreb",
  dateStyle: "medium",
  timeStyle: "short",
});

const el = (id) => document.getElementById(id);
const statusLine = el("status");

const state = { filter: "open", cursor: null, counts: null, loading: false };

function say(text, isError = false) {
  statusLine.textContent = text;
  statusLine.classList.toggle("error", isError);
}

function show(section) {
  el("signedOut").hidden = section !== "signedOut";
  el("feedback").hidden = section !== "feedback";
  el("signOut").hidden = section === "signedOut" || section === "off";
}

const config = window.ADMIN_CONFIG;
if (!config) {
  show("off");
  say("Admin is off on this server.");
} else {
  start(config);
}

function start(firebaseConfig) {
  const auth = getAuth(initializeApp(firebaseConfig));

  el("signIn").addEventListener("click", async () => {
    say("");
    try {
      await signInWithPopup(auth, new GoogleAuthProvider());
    } catch (e) {
      if (e?.code !== "auth/popup-closed-by-user" && e?.code !== "auth/cancelled-popup-request") {
        say("Sign-in failed: " + (e?.code ?? "unknown error"), true);
      }
    }
  });
  el("signOut").addEventListener("click", () => signOut(auth));
  el("more").addEventListener("click", () => load(false));

  onAuthStateChanged(auth, (user) => {
    if (user) {
      show("feedback");
      say("");
      load(true);
    } else {
      show("signedOut");
      el("list").replaceChildren();
      state.counts = null;
    }
  });

  // getIdToken() refreshes the token when it is near expiry; a 401 still
  // gets one forced refresh and a single retry.
  async function api(path, options = {}) {
    const user = auth.currentUser;
    if (!user) throw new ApiError(401);
    const send = async (force) => {
      const token = await user.getIdToken(force);
      return fetch(path, {
        ...options,
        headers: { ...options.headers, Authorization: "Bearer " + token },
      });
    };
    let res = await send(false);
    if (res.status === 401) res = await send(true);
    if (!res.ok) throw new ApiError(res.status);
    return res.json();
  }

  async function load(reset) {
    if (state.loading) return;
    state.loading = true;
    if (reset) state.cursor = null;
    const params = new URLSearchParams({ limit: String(PAGE_SIZE) });
    if (state.filter !== "all") params.set("status", state.filter);
    if (state.cursor) params.set("cursor", state.cursor);
    try {
      const page = await api("/v1/admin/feedback?" + params);
      state.counts = page.counts;
      state.cursor = page.nextCursor;
      renderChips();
      if (reset) el("list").replaceChildren();
      for (const item of page.items) el("list").append(card(item));
      if (reset && page.items.length === 0) el("list").append(empty());
      el("more").hidden = !page.nextCursor;
      say("");
    } catch (e) {
      say(errorText(e, "Couldn't load feedback"), true);
    } finally {
      state.loading = false;
    }
  }

  function renderChips() {
    const counts = state.counts ?? {};
    const total = STATUSES.reduce((sum, s) => sum + (counts[s.key] ?? 0), 0);
    const chips = [...STATUSES, { key: "all", label: "All" }].map((s) => {
      const b = document.createElement("button");
      b.className = "chip";
      b.setAttribute("role", "tab");
      b.setAttribute("aria-selected", String(s.key === state.filter));
      b.textContent = `${s.label} ${s.key === "all" ? total : counts[s.key] ?? 0}`;
      b.addEventListener("click", () => {
        if (state.filter === s.key) return;
        state.filter = s.key;
        load(true);
      });
      return b;
    });
    el("chips").replaceChildren(...chips);
  }

  function card(item) {
    const li = document.createElement("li");
    li.className = "card";

    const message = document.createElement("p");
    message.className = "message";
    message.textContent = item.message;

    const meta = document.createElement("p");
    meta.className = "meta";
    const who = item.account === "guest" ? "Guest" : "Account " + item.account;
    meta.textContent = [
      dateFormat.format(new Date(item.createdAt)),
      "App " + (item.appVersion ?? "?"),
      item.osVersion ?? "Android ?",
      item.deviceModel ?? "device ?",
      who,
    ].join(" · ");

    const controls = document.createElement("div");
    controls.className = "controls";
    const saved = document.createElement("span");
    saved.className = "saved";
    saved.setAttribute("aria-live", "polite");

    const select = document.createElement("select");
    select.id = "status-" + item.id;
    for (const s of STATUSES) {
      const option = document.createElement("option");
      option.value = s.key;
      option.textContent = s.label;
      option.selected = s.key === item.status;
      select.append(option);
    }
    let currentStatus = item.status;
    select.addEventListener("change", async () => {
      const next = select.value;
      if (await save(item.id, { status: next }, saved)) {
        moveCount(currentStatus, next);
        currentStatus = next;
      } else {
        select.value = currentStatus;
      }
    });

    const note = document.createElement("input");
    note.id = "note-" + item.id;
    note.maxLength = 500;
    note.placeholder = "e.g. fixed in 0.2.5";
    note.value = item.note ?? "";
    let currentNote = note.value;
    note.addEventListener("change", async () => {
      const next = note.value.trim();
      if (next === currentNote) return;
      if (await save(item.id, { note: next }, saved)) currentNote = next;
    });
    note.addEventListener("keydown", (e) => {
      if (e.key === "Enter") note.blur();
    });

    controls.append(label("Status", select), select, label("Note", note), note, saved);
    li.append(message, meta, controls);
    return li;
  }

  async function save(id, body, saved) {
    saved.classList.remove("error");
    saved.textContent = "Saving…";
    try {
      await api("/v1/admin/feedback/" + encodeURIComponent(id), {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      saved.textContent = "Saved";
      return true;
    } catch (e) {
      saved.classList.add("error");
      saved.textContent = errorText(e, "Not saved");
      return false;
    }
  }

  // Keeps the chip counts right without a reload; the card stays where
  // it is until the next load, so a mis-tap is easy to undo.
  function moveCount(from, to) {
    if (!state.counts) return;
    state.counts[from] = Math.max(0, (state.counts[from] ?? 0) - 1);
    state.counts[to] = (state.counts[to] ?? 0) + 1;
    renderChips();
  }
}

class ApiError extends Error {
  constructor(status) {
    super("HTTP " + status);
    this.status = status;
  }
}

function errorText(e, prefix) {
  if (e instanceof ApiError) {
    if (e.status === 403) return "This account is not an admin.";
    if (e.status === 401) return "Signed out. Sign in again.";
    if (e.status === 429) return prefix + ": too many requests, wait a minute.";
    return prefix + " (HTTP " + e.status + ").";
  }
  return prefix + ": no connection.";
}

function label(text, control) {
  const l = document.createElement("label");
  l.htmlFor = control.id;
  l.textContent = text;
  return l;
}

function empty() {
  const li = document.createElement("li");
  li.className = "empty";
  li.textContent = "Nothing here.";
  return li;
}
