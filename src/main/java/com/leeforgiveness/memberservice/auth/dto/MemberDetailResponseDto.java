package com.leeforgiveness.memberservice.auth.dto;

import com.leeforgiveness.memberservice.auth.vo.MemberDetailResponseVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDetailResponseDto {

    private String email;
    private String name;
    private String phoneNum;
    private String handle;
    private String profileImage;

    public static MemberDetailResponseVo dtoToVo(MemberDetailResponseDto memberDetailResponseDto) {
        return new MemberDetailResponseVo(memberDetailResponseDto.getEmail(),
            memberDetailResponseDto.getName(),
            memberDetailResponseDto.getPhoneNum(),
            memberDetailResponseDto.getHandle(), memberDetailResponseDto.getProfileImage());
    }
}
