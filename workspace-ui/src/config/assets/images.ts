import avatarMale from "@/assets/images/avatars/avatar-male.png";
import avatarFemale from "@/assets/images/avatars/avatar-female.png";
import avatarDefault from "@/assets/images/avatars/avatar-default.png";
import bg1 from "@/assets/images/bg/bg-1.png";
import bg2 from "@/assets/images/bg/bg-2.png";
import bg3 from "@/assets/images/bg/bg-3.png";

export interface AvatarOption {
    key: string;
    src: string;
}

export const avatarOptions: AvatarOption[] = [
    { key: "avatar-male", src: avatarMale },
    { key: "avatar-female", src: avatarFemale },
];

const avatarMap = new Map(avatarOptions.map((item) => [item.key, item.src]));
avatarMap.set("avatar-default", avatarDefault);

export const profileBackgrounds = [bg1, bg2, bg3];

export function resolveAvatar(key?: string | null, gender?: number | null) {
    if (key && avatarMap.has(key)) {
        return avatarMap.get(key)!;
    }
    return gender === 2 ? avatarFemale : gender === 1 ? avatarMale : avatarDefault;
}
