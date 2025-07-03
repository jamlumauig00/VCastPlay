// Your Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyChefZJV19wKxWpf8HV6yOVUWK95Cz9Ok",
  authDomain: "webview-try-96e1f.firebaseapp.com",
  databaseURL: "https://webview-try-96e1f-default-rtdb.firebaseio.com",
  projectId: "webview-try-96e1f",
  storageBucket: "webview-try-96e1f.appspot.com",
  messagingSenderId: "916736740152",
  appId: "1:916736740152:web:fad234ce2203f799cf8fcd",
  measurementId: "G-JEV9D39BGY"
};
firebase.initializeApp(firebaseConfig);
const db = firebase.database();

const currentMediaContainer = document.getElementById('current-media-container');
const loadingElement = document.getElementById('loading');
const errorMessageElement = document.getElementById('error-message');

let currentPlaylist = [];
let currentItemIndex = -1;
let currentMediaElement = null;
let imageTimer = null;

const IMAGE_DISPLAY_DURATION = 5000;

function loadMedia(item) {
  if (!item || !item.url) {
    displayError('Invalid media item.');
    return;
  }

  currentMediaContainer.innerHTML = '';
  if (imageTimer) clearTimeout(imageTimer);
  if (currentMediaElement) {
    currentMediaElement.removeEventListener('ended', playNextItem);
    currentMediaElement = null;
  }

  if (item.type === 'video') {
    const video = document.createElement('video');
    video.src = item.url;
    video.autoplay = true;
    video.muted = true;
    video.playsInline = true;

    video.addEventListener('ended', playNextItem);
    video.addEventListener('error', () => {
      displayError(`Error playing video: ${item.title}`);
      playNextItem();
    });

    currentMediaContainer.appendChild(video);
    currentMediaElement = video;

    if (window.AndroidInterface?.setVideoUrl) {
      AndroidInterface.setVideoUrl(item.url);
    }

    video.load();
    video.play().catch(e => console.warn('Autoplay issue:', e));

  } else if (item.type === 'image') {
    const img = document.createElement('img');
    img.src = item.url;
    img.alt = item.title || 'Image';

    img.addEventListener('load', () => {
      imageTimer = setTimeout(playNextItem, IMAGE_DISPLAY_DURATION);
    });

    img.addEventListener('error', () => {
      displayError(`Error loading image: ${item.title}`);
      playNextItem();
    });

    currentMediaContainer.appendChild(img);
    currentMediaElement = img;

  } else if (item.type === 'html') {
    const iframe = document.createElement('iframe');
    iframe.src = item.url;
    iframe.width = '100%';
    iframe.height = '100%';
    iframe.style.border = 'none';

    currentMediaContainer.appendChild(iframe);
    currentMediaElement = iframe;

    imageTimer = setTimeout(playNextItem, 15000);
  } else {
    displayError(`Unsupported media type: ${item.type}`);
    playNextItem();
  }
}

function playNextItem() {
  if (!currentPlaylist.length) {
    displayError('Playlist is empty.');
    return;
  }

  currentItemIndex++;
  if (currentItemIndex >= currentPlaylist.length) {
    currentItemIndex = 0;
  }
  loadMedia(currentPlaylist[currentItemIndex]);
}

function displayError(message) {
  errorMessageElement.textContent = message;
  errorMessageElement.style.display = 'block';
  loadingElement.style.display = 'none';
  currentMediaContainer.innerHTML = '';
}

function hideError() {
  errorMessageElement.style.display = 'none';
}

function fetchAndRenderPlaylist(deviceKey) {
  loadingElement.style.display = 'block';
  hideError();
  currentMediaContainer.innerHTML = '';
  currentPlaylist = [];
  currentItemIndex = -1;

  const deviceRef = db.ref('vcast').child(deviceKey);

  deviceRef.on('value', snapshot => {
    const data = snapshot.val();
    if (data && Array.isArray(data.medias) && data.medias.length > 0) {
      currentPlaylist = data.medias;
      currentItemIndex = 0;
      loadMedia(currentPlaylist[currentItemIndex]);
    } else {
      displayError('No content available for this device.');
    }

    loadingElement.style.display = 'none';
  }, error => {
    console.error("Firebase error:", error);
    displayError('Failed to load content from Firebase.');
    loadingElement.style.display = 'none';
  });
}

window.addEventListener('load', () => {
  if (window.AndroidInterface?.getDeviceKey) {
    try {
      const deviceKey = AndroidInterface.getDeviceKey();
      if (deviceKey) {
        fetchAndRenderPlaylist(deviceKey);
      } else {
        displayError('Device key missing from AndroidInterface.');
      }
    } catch (e) {
      console.error("Error accessing AndroidInterface:", e);
      displayError('Error communicating with the Android app.');
    }
  } else {
    const defaultDeviceKey = 'device1';
    fetchAndRenderPlaylist(defaultDeviceKey);
  }
});
