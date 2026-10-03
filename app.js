const TEXT = {
  he: {
    weeklyGame:'המשחק המשפחתי השבועי', headline:'משחקים ביחד. נהנים ביחד.', subtitle:'בוחרים משימות, מסמנים מה הושלם וצוברים נקודות לאורך השבוע.', scoreboard:'לוח תוצאות', familyProgress:'ההתקדמות המשפחתית', familyMembers:'בני המשפחה', sundayToFriday:'ראשון עד שישי', weeklyBoard:'לוח המשימות השבועי', show:'הצגה', allMembers:'כולם', saturday:'שבת', restDay:'יום מנוחה — בלי מטלות!', thisWeek:'השבוע הנוכחי', previousWeek:'השבוע הקודם', nextWeek:'השבוע הבא', weeksAgo:'לפני {count} שבועות', weeksAhead:'בעוד {count} שבועות', weekNumber:'שבוע {number}', backToCurrentWeek:'חזרה לשבוע הנוכחי', readOnlyWeek:'צפייה בלבד', completionOnly:'ניתן לסמן השלמה', chooseChore:'בחירת משימה', familyMember:'בן או בת משפחה', availableChores:'משימות זמינות', addToDay:'הוספה ליום', yourTeam:'הצוות שלכם', memberName:'שם', nameExists:'השם הזה כבר קיים במשפחה', invalidName:'השם יכול להכיל אותיות ורווחים בלבד', add:'הוספה', welcome:'ברוכים הבאים למשחק!', addFirstMember:'הוסיפו בן או בת משפחה כדי להתחיל.', points:'נק׳', completed:'הושלמו', of:'מתוך', chores:'משימות', addChore:'הוספת משימה', noChores:'עדיין אין משימות ליום הזה', chooseMember:'נא לבחור בן או בת משפחה', chooseMemberFirst:'תחילה בוחרים בן או בת משפחה', chooseAtLeastOne:'נא לבחור משימה', memberHasChore:'לבן המשפחה כבר יש משימה ביום הזה', choreTaken:'כבר נבחרה ליום הזה', remove:'העברה לארכיון', deleteTask:'מחיקת משימה', install:'התקנת האפליקציה', confirmRemove:'להעביר את בן המשפחה לארכיון? היסטוריית המשימות תישמר.', days:['ראשון','שני','שלישי','רביעי','חמישי','שישי'], locale:'he-IL', online:'מסונכרן עם השרת', offline:'אין חיבור לשרת — המידע עשוי להיות לא מעודכן', loading:'טוען מידע משותף…', retry:'ניסיון חוזר', saveFailed:'לא ניתן לשמור כרגע. נסו שוב.', staleVersion:'המשימה השתנתה במכשיר אחר. המידע נטען מחדש.', planningClosed:'עריכת משימות פתוחה רק לשבוע הנוכחי ולשבוע הבא.'
  },
  en: {
    weeklyGame:'The weekly family game', headline:'Pitch in. Win together.', subtitle:'Choose chores, mark what’s done, and collect points throughout the week.', scoreboard:'Scoreboard', familyProgress:'Family progress', familyMembers:'Family members', sundayToFriday:'Sunday through Friday', weeklyBoard:'Weekly chore board', show:'Show', allMembers:'Everyone', saturday:'Saturday', restDay:'A day off — no chores!', thisWeek:'Current week', previousWeek:'Previous week', nextWeek:'Next week', weeksAgo:'{count} weeks ago', weeksAhead:'{count} weeks ahead', weekNumber:'Week {number}', backToCurrentWeek:'Back to current week', readOnlyWeek:'View only', completionOnly:'Completion only', chooseChore:'Choose a chore', familyMember:'Family member', availableChores:'Available chores', addToDay:'Add to day', yourTeam:'Your team', memberName:'Name', nameExists:'That name already exists in the family', invalidName:'Names may contain letters and spaces only', add:'Add', welcome:'Welcome to the game!', addFirstMember:'Add a family member to get started.', points:'pts', completed:'completed', of:'of', chores:'chores', addChore:'Add chore', noChores:'No chores here yet', chooseMember:'Please choose a family member', chooseMemberFirst:'Choose a family member first', chooseAtLeastOne:'Please choose a chore', memberHasChore:'This family member already has a chore that day', choreTaken:'Already chosen for this day', remove:'Archive', deleteTask:'Delete chore', install:'Install app', confirmRemove:'Archive this family member? Chore history will be preserved.', days:['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday'], locale:'en-US', online:'Synced with server', offline:'Server unavailable — data may be out of date', loading:'Loading shared data…', retry:'Retry', saveFailed:'Could not save right now. Please try again.', staleVersion:'The chore changed on another device. The latest data was loaded.', planningClosed:'Chore editing is open only for the current and next week.'
  }
};
Object.assign(TEXT.he,{setupWelcome:'התחלה חדשה',setupFamily:'הגדרת המשפחה',setupInstructions:'לפני שמתחילים, מגדירים את שם המשפחה, שפת ברירת המחדל ובני המשפחה.',familyName:'שם המשפחה',defaultLanguage:'שפת ברירת מחדל',initialMembers:'בני המשפחה',addAnotherMember:'הוספת בן משפחה נוסף',startFamily:'יצירת המשפחה והתחלה'});
Object.assign(TEXT.en,{setupWelcome:'A fresh start',setupFamily:'Set up your family',setupInstructions:'Before opening the chore board, choose the family name, default language, and family members.',familyName:'Family name',defaultLanguage:'Default language',initialMembers:'Family members',addAnotherMember:'Add another member',startFamily:'Create family and start'});
Object.assign(TEXT.he,{rights:'זכויות',chooseRight:'בחירת זכות',forMember:'עבור',availableBalance:'יתרה זמינה: {points} נק׳',redeemRight:'בחירת זכות',noRights:'עדיין לא הוגדרו זכויות.',noAffordableRights:'אין כרגע זכויות שמתאימות ליתרת הנקודות.',notEnoughPoints:'אין מספיק נקודות לזכות הזו.',confirmRight:'לבחור את הזכות הזו עבור {name} בעלות של {cost} נקודות?',pointsAlreadySpent:'לא ניתן לבטל את המשימה כי הנקודות שלה כבר נוצלו.'});
Object.assign(TEXT.en,{rights:'Rights',chooseRight:'Choose a right',forMember:'For',availableBalance:'Available balance: {points} pts',redeemRight:'Choose right',noRights:'No rights have been configured yet.',noAffordableRights:'There are currently no rights this member can afford.',notEnoughPoints:'There are not enough points for this right.',confirmRight:'Choose this right for {name} at a cost of {cost} points?',pointsAlreadySpent:'This chore cannot be uncompleted because its points have already been spent.'});
Object.assign(TEXT.he,{currentBalance:'יתרת נקודות',totalEarned:'סה״כ נקודות ממשימות',rightsCashed:'זכויות שנבחרו',rightsValue:'בשווי {points} נק׳'});
Object.assign(TEXT.en,{currentBalance:'Point balance',totalEarned:'Total chore points',rightsCashed:'Rights redeemed',rightsValue:'worth {points} pts'});
Object.assign(TEXT.he,{weekendOnly:'לסוף השבוע בלבד',allWeek:'כל ימות השבוע',dailyUses:'{used} מתוך {cap} היום',noAvailableRights:'אין כרגע זכויות זמינות ומתאימות ליתרת הנקודות.',rightUnavailableToday:'הזכות אינה זמינה היום.',dailyCapReached:'מכסת הזכות היומית נוצלה.'});
Object.assign(TEXT.en,{weekendOnly:'Weekends only',allWeek:'All week',dailyUses:'{used} of {cap} today',noAvailableRights:'There are currently no available rights this member can afford.',rightUnavailableToday:'This right is not available today.',dailyCapReached:'This right’s daily limit has been reached.'});
Object.assign(TEXT.he,{mainSections:'מדורי המשחק'});
Object.assign(TEXT.en,{mainSections:'Game sections'});
Object.assign(TEXT.he,{login:'כניסה',logout:'יציאה',welcomeBack:'ברוכים השבים',pin:'קוד אישי (4–6 ספרות)',pinHeading:'קוד אישי',pinRequirements:'הקוד האישי חייב להכיל 4–6 ספרות בלבד',role:'תפקיד',parent:'הורה',child:'ילד/ה',invalidLogin:'שם או קוד אישי שגויים',loginLocked:'יותר מדי ניסיונות כניסה. נסו שוב בעוד 15 דקות.'});
Object.assign(TEXT.en,{login:'Log in',logout:'Log out',welcomeBack:'Welcome back',pin:'PIN (4–6 digits)',pinHeading:'PIN',pinRequirements:'The PIN must contain 4–6 digits only',role:'Role',parent:'Parent',child:'Child',invalidLogin:'Incorrect member or PIN',loginLocked:'Too many login attempts. Try again in 15 minutes.'});

const PREFERENCES_KEY = 'familyChoreGame.preferences.v2';
const legacy = readJson('familyChoreGame.v1') || {};
const preferences = readJson(PREFERENCES_KEY) || {};
let hasLanguagePreference = Boolean(preferences.language || legacy.language);
const state = {
  language: preferences.language || legacy.language || 'he',
  weekOffset: Number.isInteger(preferences.weekOffset) ? preferences.weekOffset : (Number.isInteger(legacy.weekOffset) ? legacy.weekOffset : 0),
  filter: preferences.filter || legacy.filter || 'all',
  activeTab: 'chores',
  members: [], allMembers: [], categories: [], chores: [], rights: [], balances: [], rightRedemptions: [],
  rightMemberId: null, today: null, todayNonSchoolDay: false, board: null, currentWeekStart: null,
  online: false, loading: true, error: null, user: null
};
let activeDay = 0;
let installPrompt;
let loadSequence = 0;

const $ = selector => document.querySelector(selector);
const t = key => TEXT[state.language][key] ?? key;
const member = id => state.allMembers.find(item => item.id === id);
const chore = id => state.chores.find(item => item.id === id);
const categoryName = item => state.language === 'he' ? item.nameHe : item.nameEn;
const choreName = item => state.language === 'he' ? item.nameHe : item.nameEn;

function readJson(key) {
  try { return JSON.parse(localStorage.getItem(key) || 'null'); } catch { return null; }
}
function savePreferences() {
  localStorage.setItem(PREFERENCES_KEY, JSON.stringify({language:state.language, weekOffset:state.weekOffset, filter:state.filter}));
}
function parseDate(value) { return new Date(`${value}T12:00:00`); }
function iso(date) {
  const year=date.getFullYear(), month=String(date.getMonth()+1).padStart(2,'0'), day=String(date.getDate()).padStart(2,'0');
  return `${year}-${month}-${day}`;
}
function addDays(value, days) { const date=parseDate(value); date.setDate(date.getDate()+days); return iso(date); }
function displayedWeekStart() { return addDays(state.currentWeekStart, state.weekOffset*7); }
function dayDate(index) { return parseDate(addDays(displayedWeekStart(), index)); }
function initials(name) { return name.trim().split(/\s+/).slice(0,2).map(part=>part[0]).join('').toUpperCase(); }
function makeRequestId() { return crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`; }
function escapeHtml(value) { return String(value).replace(/[&<>'"]/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char])); }

async function api(path, options={}) {
  const method=(options.method||'GET').toUpperCase();
  const csrfToken=document.cookie.split('; ').find(cookie=>cookie.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('=');
  const response = await fetch(`/api/v1${path}`, {
    ...options,
    headers: {'Accept':'application/json', ...(options.body ? {'Content-Type':'application/json'} : {}),
      ...(!['GET','HEAD','OPTIONS'].includes(method)&&csrfToken?{'X-XSRF-TOKEN':decodeURIComponent(csrfToken)}:{}), ...options.headers}
  });
  if (!response.ok) {
    let payload={};
    try { payload=await response.json(); } catch { /* response has no JSON body */ }
    const error=new Error(payload.message || `Request failed (${response.status})`);
    error.code=payload.code; error.status=response.status;
    throw error;
  }
  return response.status===204 ? null : response.json();
}

async function bootstrap() {
  state.loading=true; state.error=null; renderStatus();
  try {
    const data=await api('/bootstrap');
    $('#gameView').classList.remove('hidden');
    if(!hasLanguagePreference) state.language=data.defaultLanguage;
    state.allMembers=data.members;
    state.members=data.members.filter(item=>item.active);
    state.categories=data.categories;
    state.chores=data.chores;
    state.rights=data.rights;
    state.balances=data.balances;
    state.rightRedemptions=data.rightRedemptions;
    state.today=data.today;
    state.todayNonSchoolDay=data.todayNonSchoolDay;
    if(state.user?.role==='CHILD') state.rightMemberId=state.user.memberId;
    else if(!state.members.some(item=>item.id===state.rightMemberId)) state.rightMemberId=state.members[0]?.id||null;
    state.currentWeekStart=data.currentWeek.weekStart;
    state.online=true;
    await loadWeek(state.weekOffset, data);
  } catch (error) {
    state.online=false; state.error=error; state.loading=false; render();
  }
}

function openSetup() {
  const dialog=$('#setupDialog');
  $('#setupLanguage').value=state.language;
  if(!dialog.open) dialog.showModal();
  if(!$('#setupMembers').children.length) addSetupMemberRow('',true);
}
function addSetupMemberRow(value='',parent=false) {
  const row=document.createElement('div'); row.className='setup-member-row';
  row.innerHTML=`<input class="setup-member-name" maxlength="24" autocomplete="name" required value="${escapeHtml(value)}" placeholder="${t('memberName')}"><input class="setup-member-pin" type="password" inputmode="numeric" pattern="[0-9]{4,6}" minlength="4" maxlength="6" required placeholder="${t('pin')}"><select class="setup-member-role"><option value="PARENT" ${parent?'selected':''}>${t('parent')}</option><option value="CHILD" ${parent?'':'selected'}>${t('child')}</option></select><button type="button" data-remove-setup-member aria-label="${t('remove')}">×</button>`;
  $('#setupMembers').append(row);
}

async function start() {
  try {
    const session=await api('/session');
    if(session.setupRequired){state.online=true;state.loading=false;$('#gameView').classList.add('hidden');openSetup();renderStatus();return;}
    if(!session.authenticated){await openLogin();return;}
    state.user=session; $('#currentUser').textContent=session.displayName; $('#currentUser').classList.remove('hidden'); $('#logoutButton').classList.remove('hidden');
    await bootstrap();
  } catch(error){state.online=false;state.loading=false;state.error=error;renderStatus();}
}
async function openLogin(){
  const members=await api('/login-members'); state.online=true;state.loading=false;$('#gameView').classList.add('hidden');
  $('#loginMember').innerHTML=members.map(item=>`<option value="${item.id}">${escapeHtml(item.displayName)}</option>`).join('');
  applyLanguage();renderStatus();if(!$('#loginDialog').open)$('#loginDialog').showModal();
}

async function loadWeek(offset, bootstrapData=null) {
  if (!state.currentWeekStart) return;
  const sequence=++loadSequence;
  state.weekOffset=offset; state.loading=true; state.error=null; savePreferences(); renderStatus();
  try {
    let board;
    if (bootstrapData && offset===0) board=bootstrapData.currentWeek;
    else if (bootstrapData && offset===1) board=bootstrapData.comingWeek;
    else board=await api(`/weeks/${displayedWeekStart()}`);
    if (sequence!==loadSequence) return;
    state.board=board; state.online=true; state.loading=false;
    if (state.filter!=='all' && !state.members.some(item=>item.id===state.filter)) state.filter='all';
    render();
  } catch (error) {
    if (sequence!==loadSequence) return;
    state.online=false; state.error=error; state.loading=false; render();
  }
}

async function refresh() {
  return bootstrap();
}

function applyLanguage() {
  const lang=state.language;
  document.documentElement.lang=lang; document.documentElement.dir=lang==='he'?'rtl':'ltr';
  document.querySelectorAll('[data-i18n]').forEach(el=>el.textContent=t(el.dataset.i18n));
  document.querySelectorAll('[data-i18n-aria]').forEach(el=>el.setAttribute('aria-label',t(el.dataset.i18nAria)));
  document.querySelectorAll('[data-i18n-tooltip]').forEach(el=>el.dataset.tooltip=t(el.dataset.i18nTooltip));
  $('#languageButton').textContent=lang==='he'?'English':'עברית'; $('#installButton').textContent=t('install');
}
function selectMainTab(tab, focus=false) {
  if (!['chores','rights'].includes(tab)) tab='chores';
  state.activeTab=tab;
  document.querySelectorAll('[data-main-tab]').forEach(button=>{
    const active=button.dataset.mainTab===tab;
    button.classList.toggle('active',active);
    button.setAttribute('aria-selected',String(active));
    button.tabIndex=active?0:-1;
    document.getElementById(button.getAttribute('aria-controls')).hidden=!active;
    if(active&&focus)button.focus();
  });
}
function render() {
  applyLanguage(); selectMainTab(state.activeTab); renderStatus();
  if (!state.currentWeekStart) return;
  renderWeekLabel(); renderMembers(); renderScores(); renderBoard(); renderRights(); savePreferences();
}
function renderStatus() {
  const banner=$('#syncStatus');
  if (!banner) return;
  banner.className=`sync-status ${state.online?'online':'offline'}`;
  banner.innerHTML=state.loading
    ? `<span class="sync-dot"></span>${t('loading')}`
    : state.online
      ? `<span class="sync-dot"></span>${t('online')}`
      : `<span class="sync-dot"></span>${t('offline')} <button id="retrySync" type="button">${t('retry')}</button>`;
}
function renderWeekLabel() {
  const start=parseDate(displayedWeekStart()), end=dayDate(5), format=new Intl.DateTimeFormat(t('locale'),{day:'numeric',month:'short'});
  $('#weekLabel').textContent=`${format.format(start)} – ${format.format(end)}`;
  const relative=state.weekOffset===0?t('thisWeek'):state.weekOffset===-1?t('previousWeek'):state.weekOffset===1?t('nextWeek'):state.weekOffset<0?t('weeksAgo').replace('{count}',Math.abs(state.weekOffset)):t('weeksAhead').replace('{count}',state.weekOffset);
  $('#weekStatus').textContent=`${t('weekNumber').replace('{number}',getWeekNumber(start))} · ${relative}`;
  $('#todayButton').classList.toggle('hidden',state.weekOffset===0);
}
function getWeekNumber(date) { const target=new Date(Date.UTC(date.getFullYear(),date.getMonth(),date.getDate())); const day=target.getUTCDay()||7; target.setUTCDate(target.getUTCDate()+4-day); const yearStart=new Date(Date.UTC(target.getUTCFullYear(),0,1)); return Math.ceil((((target-yearStart)/86400000)+1)/7); }
function renderMembers() {
  const options=[`<option value="all">${t('allMembers')}</option>`,...state.members.map(m=>`<option value="${m.id}">${escapeHtml(m.displayName)}</option>`)].join('');
  $('#memberFilter').innerHTML=options; $('#memberFilter').value=state.filter;
  if(!$('#taskDialog').open) $('#taskMember').innerHTML=`<option value="">—</option>${state.members.map(m=>`<option value="${m.id}">${escapeHtml(m.displayName)}</option>`).join('')}`;
  const rightMembers=state.user?.role==='PARENT'?state.members:state.members.filter(m=>m.id===state.user?.memberId);
  $('#rightMember').innerHTML=rightMembers.map(m=>`<option value="${m.id}">${escapeHtml(m.displayName)}</option>`).join('');
  $('#rightMember').value=state.rightMemberId||'';
  $('#membersList').innerHTML=state.members.map(m=>`<div class="member-row" style="--member-color:${m.color}"><span class="mini-avatar">${escapeHtml(initials(m.displayName))}</span><strong>${escapeHtml(m.displayName)} · ${t(m.role==='PARENT'?'parent':'child')}</strong><button type="button" class="remove-member" data-remove-member="${m.id}" ${state.online?'':'disabled'}>${t('remove')}</button></div>`).join('');
  $('#manageMembersButton').classList.toggle('hidden',state.user?.role!=='PARENT');
}
function renderRights() {
  const selected=state.members.find(item=>item.id===state.rightMemberId), balance=state.balances.find(item=>item.memberId===state.rightMemberId), available=balance?.availablePoints||0;
  $('#rightBalance').textContent=selected?t('availableBalance').replace('{points}',available):'';
  if(!state.rights.length){$('#rightCards').innerHTML=`<div class="empty-state"><span aria-hidden="true">🎁</span><p>${t('noRights')}</p></div>`;return;}
  const options=selected?state.rights.map(item=>{
    const used=state.rightRedemptions.filter(redemption=>redemption.memberId===selected.id&&redemption.rightId===item.id&&dateInJerusalem(redemption.createdAt)===state.today).length;
    return {item,used};
  }).filter(option=>available>=option.item.pointCost&&(option.item.availabilityType==='ALL_WEEK'||state.todayNonSchoolDay)&&option.used<option.item.dailyCap):[];
  if(!options.length){$('#rightCards').innerHTML=`<div class="empty-state"><span aria-hidden="true">🎁</span><p>${t('noAvailableRights')}</p></div>`;return;}
  const sorted=options.sort((left,right)=>choreName(left.item).localeCompare(choreName(right.item),t('locale'),{sensitivity:'base'}));
  $('#rightCards').innerHTML=sorted.map(({item,used})=>`<article class="right-card"><div><h3>${escapeHtml(choreName(item))}</h3><div class="right-meta"><span class="points-pill">${item.pointCost} ${t('points')}</span><span>${item.availabilityType==='WEEKEND'?t('weekendOnly'):t('allWeek')}</span><span>${t('dailyUses').replace('{used}',used).replace('{cap}',item.dailyCap)}</span></div></div><button type="button" class="button primary" data-redeem-right="${item.id}" ${state.online?'':'disabled'}>${t('redeemRight')}</button></article>`).join('');
}
function dateInJerusalem(value) {
  const parts=new Intl.DateTimeFormat('en-US',{timeZone:'Asia/Jerusalem',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date(value));
  const part=type=>parts.find(item=>item.type===type).value;
  return `${part('year')}-${part('month')}-${part('day')}`;
}
function renderScores() {
  if (!state.members.length) { const node=$('#emptyMembersTemplate').content.cloneNode(true); node.querySelector('[data-i18n="welcome"]').textContent=t('welcome'); node.querySelector('[data-i18n="addFirstMember"]').textContent=t('addFirstMember'); $('#scoreCards').replaceChildren(node); return; }
  const scoreMap=new Map((state.board?.scores||[]).map(score=>[score.memberId,score]));
  const balanceMap=new Map(state.balances.map(balance=>[balance.memberId,balance]));
  $('#scoreCards').innerHTML=state.members.map(m=>{
    const score=scoreMap.get(m.id)||{completedCount:0,assignmentCount:0,points:0}, balance=balanceMap.get(m.id)||{earnedPoints:0,spentPoints:0,availablePoints:0}, redemptions=state.rightRedemptions.filter(item=>item.memberId===m.id), redeemedValue=redemptions.reduce((sum,item)=>sum+item.pointCostSnapshot,0), progress=score.assignmentCount?Math.round(score.completedCount/score.assignmentCount*100):0;
    return `<article class="score-card" style="--member-color:${m.color}"><div class="avatar">${escapeHtml(initials(m.displayName))}</div><h3>${escapeHtml(m.displayName)}</h3><div class="score-label">${t('currentBalance')}</div><div class="score-value"><strong>${balance.availablePoints}</strong><span>${t('points')}</span></div><div class="score-details"><div><span>${t('totalEarned')}</span><strong>${balance.earnedPoints} ${t('points')}</strong></div><div><span>${t('rightsCashed')}</span><strong>${redemptions.length} · ${t('rightsValue').replace('{points}',redeemedValue)}</strong></div></div><div class="progress-copy">${score.completedCount} ${t('of')} ${score.assignmentCount} ${t('completed')}</div><div class="progress-track"><div class="progress-fill" style="width:${progress}%"></div></div></article>`;
  }).join('');
}
function renderBoard() {
  const today=iso(new Date()), formatter=new Intl.DateTimeFormat(t('locale'),{day:'numeric',month:'short'}), canPlan=state.user?.role==='PARENT'&&['planning','execution'].includes(state.board?.mode)&&state.online, canComplete=task=>state.board?.mode==='execution'&&state.online&&(state.user?.role==='PARENT'||task.memberId===state.user?.memberId), assignments=state.board?.assignments||[];
  $('#weekBoard').innerHTML=Array.from({length:6},(_,index)=>{ const date=dayDate(index), dateKey=iso(date), dayInfo=state.board?.days?.find(item=>item.date===dateKey), nonSchool=dayInfo?.nonSchoolDay===true, dayKind=dayInfo?(state.language==='he'?dayInfo.nameHe:dayInfo.nameEn):'', visible=assignments.filter(task=>task.assignmentDate===dateKey&&(state.filter==='all'||task.memberId===state.filter)); const cards=visible.map(task=>{ const c=chore(task.choreId),m=member(task.memberId),done=task.status==='completed'; if(!c||!m)return ''; return `<article class="task-card ${done?'done':''}" style="--member-color:${m.color}"><div class="task-top"><button class="task-check" data-toggle-task="${task.id}" aria-label="${t('completed')}" ${canComplete(task)?'':'disabled'}>${done?'✓':''}</button><span class="task-name">${escapeHtml(choreName(c))}</span></div><div class="task-meta"><span>${escapeHtml(m.displayName)} · <span class="points-pill">${task.pointsSnapshot} ${t('points')}</span></span>${canPlan?`<button class="delete-task" data-delete-task="${task.id}">${t('deleteTask')}</button>`:''}</div></article>`; }).join(''); const actionLabel=canPlan?`＋ ${t('addChore')}`:state.board?.mode==='execution'?t('completionOnly'):t('readOnlyWeek'); return `<section class="day-column ${nonSchool?'non-school-day':'school-day'} ${dateKey===today?'today':''}"><div class="day-header"><div><h3>${t('days')[index]}</h3><span class="day-kind">${escapeHtml(dayKind)}</span></div><span class="day-date">${formatter.format(date)}</span></div><div class="task-list">${cards||`<p class="empty-day">${t('noChores')}</p>`}</div><button class="add-task" data-add-day="${index}" ${canPlan?'':'disabled'}>${actionLabel}</button></section>`; }).join('');
}
function openTaskDialog(day) {
  if (!['planning','execution'].includes(state.board?.mode)||!state.online) return;
  if (!state.members.length) { $('#membersDialog').showModal(); return; }
  activeDay=Number(day); const dateKey=iso(dayDate(activeDay)), dayTasks=state.board.assignments.filter(task=>task.assignmentDate===dateKey), busyMembers=new Set(dayTasks.map(task=>task.memberId));
  $('#taskDialogDay').textContent=`${t('days')[activeDay]} · ${new Intl.DateTimeFormat(t('locale'),{day:'numeric',month:'long'}).format(dayDate(activeDay))}`;
  $('#taskMember').innerHTML=`<option value="">—</option>${state.members.map(m=>`<option value="${m.id}" ${busyMembers.has(m.id)?'disabled':''}>${escapeHtml(m.displayName)}${busyMembers.has(m.id)?` — ${t('memberHasChore')}`:''}</option>`).join('')}`;
  $('#choreChoices').dataset.date=dateKey; $('#choreChoices').innerHTML=`<p class="choose-member-first">${t('chooseMemberFirst')}</p>`; $('#taskError').textContent=''; $('#taskDialog').showModal();
}
function renderChoreChoices() {
  const dateKey=$('#choreChoices').dataset.date, taken=new Set(state.board.assignments.filter(task=>task.assignmentDate===dateKey).map(task=>task.choreId));
  $('#choreChoices').innerHTML=state.categories.map(category=>`<section class="chore-category"><h3>${escapeHtml(categoryName(category))}</h3>${state.chores.filter(c=>c.categoryId===category.id).map(c=>`<label class="chore-option ${taken.has(c.id)?'unavailable':''}"><input type="radio" name="chore" value="${c.id}" ${taken.has(c.id)?'disabled':''}><span>${escapeHtml(choreName(c))}${taken.has(c.id)?` · ${t('choreTaken')}`:''}</span><span class="points-pill">${c.points} ${t('points')}</span></label>`).join('')}</section>`).join('');
}
function userError(error) {
  if (error.code==='stale_version') return t('staleVersion');
  if (error.code==='planning_closed'||error.code==='completion_closed') return t('planningClosed');
  if (error.code==='constraint_conflict') return t('choreTaken');
  if (error.code==='insufficient_points') return t('notEnoughPoints');
  if (error.code==='points_already_spent') return t('pointsAlreadySpent');
  if (error.code==='right_not_available_today') return t('rightUnavailableToday');
  if (error.code==='daily_cap_reached') return t('dailyCapReached');
  return t('saveFailed');
}

$('#languageButton').addEventListener('click',()=>{state.language=state.language==='he'?'en':'he';hasLanguagePreference=true;render();});
$('#previousWeek').addEventListener('click',()=>loadWeek(state.weekOffset-1));
$('#nextWeek').addEventListener('click',()=>loadWeek(state.weekOffset+1));
$('#todayButton').addEventListener('click',()=>loadWeek(0));
$('#memberFilter').addEventListener('change',event=>{state.filter=event.target.value;render();});
$('#rightMember').addEventListener('change',event=>{state.rightMemberId=event.target.value;$('#rightError').textContent='';renderRights();});
$('#taskMember').addEventListener('change',event=>{if(event.target.value)renderChoreChoices();else $('#choreChoices').innerHTML=`<p class="choose-member-first">${t('chooseMemberFirst')}</p>`;$('#taskError').textContent='';});
$('#manageMembersButton').addEventListener('click',()=>$('#membersDialog').showModal());
$('#addSetupMember').addEventListener('click',()=>addSetupMemberRow('',false));
$('#setupDialog').addEventListener('cancel',event=>event.preventDefault());
$('#loginDialog').addEventListener('cancel',event=>event.preventDefault());
document.querySelectorAll('[data-main-tab]').forEach(button=>{
  button.addEventListener('click',()=>selectMainTab(button.dataset.mainTab));
  button.addEventListener('keydown',event=>{
    const tabs=[...document.querySelectorAll('[data-main-tab]')];
    const current=tabs.indexOf(button);
    let next=null;
    if(event.key==='ArrowLeft'||event.key==='ArrowUp')next=(current-1+tabs.length)%tabs.length;
    if(event.key==='ArrowRight'||event.key==='ArrowDown')next=(current+1)%tabs.length;
    if(event.key==='Home')next=0;
    if(event.key==='End')next=tabs.length-1;
    if(next!==null){event.preventDefault();selectMainTab(tabs[next].dataset.mainTab,true);}
  });
});

document.addEventListener('click',async event=>{
  if (event.target.closest('#retrySync')) { await bootstrap(); return; }
  const close=event.target.closest('[data-close]'); if(close) document.getElementById(close.dataset.close).close();
  const add=event.target.closest('[data-add-day]'); if(add) openTaskDialog(add.dataset.addDay);
  const toggle=event.target.closest('[data-toggle-task]');
  if(toggle&&state.board?.mode==='execution'&&state.online){ const task=state.board.assignments.find(item=>item.id===toggle.dataset.toggleTask); toggle.disabled=true; try { await api(`/assignments/${task.id}/completion`,{method:'PATCH',body:JSON.stringify({completed:task.status!=='completed',version:task.version})}); await refresh(); } catch(error) { await refresh(); alert(userError(error)); } }
  const del=event.target.closest('[data-delete-task]');
  if(del&&['planning','execution'].includes(state.board?.mode)&&state.online){ del.disabled=true; try { await api(`/assignments/${del.dataset.deleteTask}`,{method:'DELETE'}); await refresh(); } catch(error) { await refresh(); alert(userError(error)); } }
  const remove=event.target.closest('[data-remove-member]');
  if(remove&&state.online&&confirm(t('confirmRemove'))){ remove.disabled=true; try { await api(`/members/${remove.dataset.removeMember}`,{method:'DELETE'}); if(state.filter===remove.dataset.removeMember)state.filter='all'; await bootstrap(); } catch(error) { alert(userError(error)); remove.disabled=false; } }
  const setupRemove=event.target.closest('[data-remove-setup-member]');
  if(setupRemove&&$('#setupMembers').children.length>1) setupRemove.closest('.setup-member-row').remove();
  const redeem=event.target.closest('[data-redeem-right]');
  if(redeem&&state.online&&state.rightMemberId){
    const right=state.rights.find(item=>item.id===redeem.dataset.redeemRight), selected=member(state.rightMemberId);
    const message=t('confirmRight').replace('{name}',selected.displayName).replace('{cost}',right.pointCost);
    if(!confirm(message))return;
    redeem.disabled=true; $('#rightError').textContent='';
    try { await api('/right-redemptions',{method:'POST',body:JSON.stringify({requestId:makeRequestId(),memberId:selected.id,rightId:right.id})}); await bootstrap(); }
    catch(error) { $('#rightError').textContent=userError(error); await bootstrap(); }
  }
});

$('#setupForm').addEventListener('submit',async event=>{
  event.preventDefault();
  const members=[...document.querySelectorAll('.setup-member-row')].map(row=>({displayName:row.querySelector('.setup-member-name').value.trim().replace(/\s+/g,' '),role:row.querySelector('.setup-member-role').value,pin:row.querySelector('.setup-member-pin').value})).filter(item=>item.displayName);
  if(!members.length){$('#setupError').textContent=t('chooseMember');return;}
  if(members.some(item=>!/^[\p{L}\p{M}]+(?: [\p{L}\p{M}]+)*$/u.test(item.displayName))){$('#setupError').textContent=t('invalidName');return;}
  if(!members.some(item=>item.role==='PARENT')){$('#setupError').textContent=t('parent');return;}
  const button=event.submitter; button.disabled=true; $('#setupError').textContent='';
  try {
    const language=$('#setupLanguage').value;
    await api('/families',{method:'POST',body:JSON.stringify({familyName:$('#setupFamilyName').value,defaultLanguage:language,members})});
    state.language=language; hasLanguagePreference=true; savePreferences(); $('#setupDialog').close(); await openLogin();
  } catch(error) { $('#setupError').textContent=error.code==='duplicate_member_name'?t('nameExists'):userError(error); }
  finally { button.disabled=false; }
});

$('#memberForm').addEventListener('submit',async event=>{
  event.preventDefault(); const input=$('#memberName'), name=input.value.trim().replace(/\s+/g,' ');
  if(!name)return;
  if(!/^[\p{L}\p{M}]+(?: [\p{L}\p{M}]+)*$/u.test(name)){$('#memberError').textContent=t('invalidName');input.focus();return;}
  $('#memberError').textContent='';
  try { await api('/members',{method:'POST',body:JSON.stringify({displayName:name,role:$('#memberRole').value,pin:$('#memberPin').value})}); input.value='';$('#memberPin').value=''; await bootstrap(); }
  catch(error) { $('#memberError').textContent=error.code==='constraint_conflict'?t('nameExists'):userError(error); input.focus(); }
});
$('#memberName').addEventListener('input',()=>{$('#memberError').textContent='';});
$('#loginForm').addEventListener('submit',async event=>{event.preventDefault();$('#loginError').textContent='';try{const session=await api('/login',{method:'POST',body:JSON.stringify({memberId:$('#loginMember').value,pin:$('#loginPin').value})});state.user=session;$('#loginPin').value='';$('#loginDialog').close();$('#currentUser').textContent=session.displayName;$('#currentUser').classList.remove('hidden');$('#logoutButton').classList.remove('hidden');await bootstrap();}catch(error){$('#loginError').textContent=t(error.code==='too_many_login_attempts'?'loginLocked':'invalidLogin');}});
$('#logoutButton').addEventListener('click',async()=>{await api('/logout',{method:'POST'});state.user=null;$('#currentUser').classList.add('hidden');$('#logoutButton').classList.add('hidden');await openLogin();});
$('#taskForm').addEventListener('submit',async event=>{
  event.preventDefault(); if(!['planning','execution'].includes(state.board?.mode)||!state.online){$('#taskDialog').close();return;}
  const memberId=$('#taskMember').value, selected=document.querySelector('input[name="chore"]:checked'), assignmentDate=iso(dayDate(activeDay));
  if(!memberId){$('#taskError').textContent=t('chooseMember');return;} if(!selected){$('#taskError').textContent=t('chooseAtLeastOne');return;}
  const button=event.submitter; button.disabled=true;
  try { await api('/assignments',{method:'POST',body:JSON.stringify({requestId:makeRequestId(),memberId,choreId:selected.value,assignmentDate})}); $('#taskDialog').close(); await refresh(); }
  catch(error) { $('#taskError').textContent=userError(error); await refresh(); }
  finally { button.disabled=false; }
});

window.addEventListener('beforeinstallprompt',event=>{event.preventDefault();installPrompt=event;$('#installButton').classList.remove('hidden');});
$('#installButton').addEventListener('click',async()=>{if(installPrompt){installPrompt.prompt();await installPrompt.userChoice;installPrompt=null;$('#installButton').classList.add('hidden');}});
window.addEventListener('online',refresh);
document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')refresh();});
setInterval(()=>{if(document.visibilityState==='visible')refresh();},15000);
if('serviceWorker' in navigator) window.addEventListener('load',()=>navigator.serviceWorker.register('./sw.js'));
applyLanguage(); renderStatus(); start();
